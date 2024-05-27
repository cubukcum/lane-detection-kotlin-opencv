package com.example.lane_detection_opencv_kotlin


import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.CvType.CV_8UC1
import org.opencv.core.Mat

import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY
import org.opencv.imgproc.Imgproc.Canny
import org.opencv.imgproc.Imgproc.Sobel
import org.opencv.imgproc.Imgproc.cvtColor

fun Mat.toSobel(): Mat {
    Sobel(this, this, CV_8UC1, 1, 0)
    return this
}

fun Mat.toSepia(): Mat {
    val sepiaKernel = Mat(4, 4, CvType.CV_32F)
    sepiaKernel.put(
        0,
        0,
        0.189, 0.769, 0.393, 0.0,
        0.168, 0.686, 0.349, 0.0,
        0.131, 0.534, 0.272, 0.0,
        0.0, 0.0, 0.0, 1.0,
    )
    Core.transform(this, this, sepiaKernel)
    return this
}

fun Mat.toGray(): Mat {
    cvtColor(this, this, COLOR_BGR2GRAY)

    return this
}

fun Mat.toCanny(): Mat {
    val tmpMat = Mat()
    Canny(this, tmpMat, 80.0, 90.0)
    return tmpMat
}

fun Mat.getEdges(): Mat {
    val gray = Mat()
    Imgproc.cvtColor(this, gray, Imgproc.COLOR_RGB2GRAY)

    val blur = Mat()
    Imgproc.GaussianBlur(gray, blur, Size(5.0, 5.0), 0.0)

    val dest = Mat()
    Imgproc.Canny(blur, dest, 50.0, 150.0)

    return dest
}

fun Mat.getSlice(source: Mat): Mat {
    val height = source.height().toDouble()
    val width = source.width().toDouble()

    val polygons: List<MatOfPoint> = listOf(
        MatOfPoint(
            Point(0.0, 900.0), // bottom left
            Point(height, 0.0),  // top left
            Point(height, 500.0),  // top right
            Point(width, height)  // bottom right
        )
    )

    val mask = Mat.zeros(source.rows(), source.cols(), 0)
    Imgproc.fillPoly(mask, polygons, Scalar(255.0))

    val dest = Mat()
    Core.bitwise_and(source, mask, dest)

    return dest
}