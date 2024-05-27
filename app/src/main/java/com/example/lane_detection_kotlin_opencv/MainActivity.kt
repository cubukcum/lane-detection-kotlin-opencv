package com.example.lane_detection_kotlin_opencv

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.lane_detection_kotlin_opencv.databinding.ActivityMainBinding
import org.opencv.android.CameraBridgeViewBase.CAMERA_ID_BACK
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import timber.log.Timber
import org.apache.commons.math3.fitting.PolynomialCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoints

class MainActivity : AppCompatActivity(), CvCameraViewListener2 {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mRGBA: Mat
    private lateinit var mCameraManager: CameraManager

    companion object {
        init {
            System.loadLibrary("opencv_java4")
            System.loadLibrary("cvcamera")
        }
        private const val CAMERA_PERMISSION_CODE = 100
    }

    private external fun openCVVersion(): String?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("OpenCV Version: ${openCVVersion()}")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mCameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            setupCamera()
        }
    }

    private fun setupCamera() {
        binding.CvCamera.setCameraIndex(CAMERA_ID_BACK)
        binding.CvCamera.setCvCameraViewListener(this)
        binding.CvCamera.setCameraPermissionGranted()
        binding.CvCamera.enableView()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupCamera()
                } else {
                    Toast.makeText(this, "Camera permission is required to use the camera.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mRGBA = Mat(height, width, CvType.CV_8UC4)
    }

    override fun onCameraViewStopped() {
        mRGBA.release()
    }

    override fun onCameraFrame(inputFrame: CvCameraViewFrame?): Mat {
        return inputFrame?.let {
            val frame = it.rgba()
            val visualizedFrame = processFrame(frame)
            visualizedFrame
        } ?: mRGBA
    }

    private fun processFrame(frame: Mat): Mat {
        val edges = getEdges(frame)
        val slice = getSlice(edges)
        val lines = getLines(slice)
        // return visualize(frame, lines)
        return edges
    }

    private fun getEdges(source: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(source, gray, Imgproc.COLOR_BGR2GRAY)
        val blur = Mat()
        Imgproc.GaussianBlur(gray, blur, Size(5.0, 5.0), 0.0)
        val edges = Mat()
        Imgproc.Canny(blur, edges, 50.0, 150.0)
        println("Size of Mat in getEdges is ${edges.size()} ")

        return edges
    }

    private fun getSlice(source: Mat): Mat {
        val height = source.height().toDouble()
        val width = source.width().toDouble()
        val polygons = listOf(
            MatOfPoint(
                Point(width * 0.4, height * 0.4),  // top left
                Point(width * 0.6, height * 0.4),  // top right
                Point(width * 0.8, height * 1.0),  // bottom right
                Point(width * 0.0, height * 1.0)   // bottom left
            )
        )
        val mask = Mat.zeros(source.rows(), source.cols(), CvType.CV_8UC1)
        Imgproc.fillPoly(mask, polygons, Scalar(255.0))
        val slice = Mat()
        Core.bitwise_and(source, mask, slice)
        println("Size of Mat in getSlice is ${slice.size()} ")

        return slice
    }

    private fun getLines(source: Mat): Pair<HoughLine, HoughLine> {
        val lines = Mat()
        Imgproc.HoughLinesP(source, lines, 1.0, Math.PI / 180, 20, 20.0, 30.0)
        val leftLine = HoughLine(source)
        val rightLine = HoughLine(source)

        for (row in 0 until lines.rows()) {
            val points = lines.get(row, 0)
            val weighted = WeightedObservedPoints()
            val fitter = PolynomialCurveFitter.create(1)
            weighted.add(points[0], points[1])
            weighted.add(points[2], points[3])
            val fitted = fitter.fit(weighted.toList())
            val slope = fitted[1]
            if (slope < 0) {
                leftLine.add(fitted)
            } else {
                rightLine.add(fitted)
            }
        }
        return Pair(leftLine, rightLine)
    }

    private fun visualize(source: Mat, lines: Pair<HoughLine, HoughLine>): Mat {
        val dest = Mat.zeros(source.size(), source.type())
        val color = Scalar(0.0, 255.0, 0.0)
        val thickness = 5

        // Draw left line
        Imgproc.line(
            dest,
            Point(lines.first.coordinates.first.y, lines.first.coordinates.first.x),
            Point(lines.first.coordinates.second.y, lines.first.coordinates.second.x),
            color,
            thickness
        )
        // Draw right line
        Imgproc.line(
            dest,
            Point(lines.second.coordinates.first.y, lines.second.coordinates.first.x),
            Point(lines.second.coordinates.second.y, lines.second.coordinates.second.x),
            color,
            thickness
        )

        val result = Mat()
        Core.addWeighted(source, 0.8, dest, 1.0, 0.0, result)
        println("Size of Mat in visualize is ${result.size()} ")

        return result
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.CvCamera.disableView()
    }

    override fun onPause() {
        super.onPause()
        binding.CvCamera.disableView()
    }

    override fun onResume() {
        super.onResume()
        binding.CvCamera.enableView()
    }
}
