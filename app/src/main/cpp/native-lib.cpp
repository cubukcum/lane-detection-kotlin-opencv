#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>
#include "ndklogger.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_lane_1detection_1opencv_1kotlin_MainActivity_openCVVersion(JNIEnv *env, jobject thiz) {
    std::string opencvVersion = cv::getVersionString();
    LOGI("OpenCV version: %s", opencvVersion.c_str());
    return env->NewStringUTF(opencvVersion.c_str());
}
