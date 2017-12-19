#include "image_processor.h"

#include <algorithm>

#include <GLES2/gl2.h>
#include <EGL/egl.h>

#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/core/ocl.hpp>

#include "common.hpp"

enum DisplayMode {
  DISP_MODE_RAW = 0,
  DISP_MODE_THRESH = 1,
  DISP_MODE_TARGETS = 2,
  DISP_MODE_TARGETS_PLUS = 3
};

struct TargetInfo {
  double centroid_x;
  double centroid_y;
  double width;
  double height;
  std::vector<cv::Point> points;
};

std::vector<TargetInfo> processImpl(int w, int h, int texOut, DisplayMode mode,
                                    int h_min, int h_max, int s_min, int s_max,
                                    int v_min, int v_max) {
  //LOGD("Image is %d x %d", w, h);
  //LOGD("H %d-%d S %d-%d V %d-%d", h_min, h_max, s_min, s_max, v_min, v_max);
  int64_t t;

  static cv::Mat input;
  input.create(h, w, CV_8UC4);

  // read
  t = getTimeMs();
  glReadPixels(0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, input.data);
  //LOGD("glReadPixels() costs %d ms", getTimeInterval(t));

  // modify
  t = getTimeMs();
  static cv::Mat hsv;
  cv::cvtColor(input, hsv, CV_RGBA2RGB);
  cv::cvtColor(hsv, hsv, CV_RGB2HSV);
  //LOGD("cvtColor() costs %d ms", getTimeInterval(t));

  t = getTimeMs();
  static cv::Mat thresh;
  cv::inRange(hsv, cv::Scalar(h_min, s_min, v_min),
              cv::Scalar(h_max, s_max, v_max), thresh);
  //LOGD("inRange() costs %d ms", getTimeInterval(t));

  t = getTimeMs();
  static cv::Mat contour_input;
  contour_input = thresh.clone();
  std::vector<std::vector<cv::Point>> contours;
  std::vector<cv::Point> convex_contour;
  std::vector<cv::Point> poly;
  std::vector<TargetInfo> targets;
  std::vector<TargetInfo> rejected_targets;
  cv::findContours(contour_input, contours, cv::RETR_EXTERNAL,
                   cv::CHAIN_APPROX_TC89_KCOS);
  double max_target_height = 0;
  int chosen_target = 0;
  for (auto &contour : contours) {
    convex_contour.clear();
    cv::convexHull(contour, convex_contour, false);
    poly.clear();
    //3.5 is the epsilon; see approxPolyDP documentation for explanation for what it does
    cv::approxPolyDP(convex_contour, poly, 3.5, true);
    // && cv::isContourConvex(poly) {was in the if statement below}
    LOGD("poly.size is: %u", poly.size());
    LOGD("contour size is %u", contours.size());

    // Originally 4, poly.size() equal to number of sides detected.
    if (poly.size() > 3 && poly.size() < 8) {
      TargetInfo target;
      int min_x = std::numeric_limits<int>::max();
      int max_x = std::numeric_limits<int>::min();
      int min_y = std::numeric_limits<int>::max();
      int max_y = std::numeric_limits<int>::min();
      target.centroid_x = 0;
      target.centroid_y = 0;
      for (auto point : poly) {
        if (point.x < min_x)
          min_x = point.x;
        if (point.x > max_x)
          max_x = point.x;
        if (point.y < min_y)
          min_y = point.y;
        if (point.y > max_y)
          max_y = point.y;
        target.centroid_x += point.x;
        target.centroid_y += point.y;
      }
      target.centroid_x /= poly.size();
      target.centroid_y /= poly.size();
      //LOGD("Max_x: %d, min_x: %d, max_y: %d, min_y: %d", max_x, min_x, max_y, min_y);
      target.width = max_x - min_x;
      target.height = max_y - min_y;
      target.points = poly;

      // Filter based on size
      // Keep in mind width/height are in imager terms...
      const double kMinTargetWidth = 1;
      const double kMaxTargetWidth = 300;
      const double kMinTargetHeight = 1;
      const double kMaxTargetHeight = 100;
      if (target.width < kMinTargetWidth || target.width > kMaxTargetWidth ||
          target.height < kMinTargetHeight ||
          target.height > kMaxTargetHeight) {
        LOGD("Rejecting target due to size, width: %lf height: %lf", target.width, target.height);
        rejected_targets.push_back(std::move(target));
        continue;
      }
      // @todo Someone figure out this math; how to calculate shape of target? It's a weird arc shape.
      // Filter based on shape


      const double kNearlyHorizontalSlope = 0.5;
      const double kNearlyVerticalSlope = 1.5;
      int num_nearly_horizontal_slope = 0;
      int num_nearly_vertical_slope = 0;
      for (int i = 0; i < poly.size(); i++) {
        double dy = target.points[i].y - target.points[(i + 1) % poly.size()].y;
        double dx = target.points[i].x - target.points[(i + 1) % poly.size()].x;
        double slope = std::numeric_limits<double>::max();
        if (dx != 0) {
          slope = dy / dx;
        }
        if (std::abs(slope) >= kNearlyVerticalSlope) {
          num_nearly_vertical_slope++;
        }
        if (std::abs(slope) <= kNearlyHorizontalSlope) {
          num_nearly_horizontal_slope++;
        }
      }
      if (num_nearly_vertical_slope < 2 || num_nearly_vertical_slope > 3 || num_nearly_horizontal_slope < 1) {
        LOGD("Rejecting target due to shape");
        LOGD("Horizontal sides: %d, vertical sides: %d", num_nearly_horizontal_slope, num_nearly_vertical_slope);
        rejected_targets.push_back(std::move(target));
        continue;
      }

        const double kminRatio = 0.28;
        const double kmaxRatio = 0.6;
        double ratio = 0;
        if (target.width != 0) {
          ratio = target.height / target.width;
        }
        if (ratio > kmaxRatio || ratio < kminRatio) {
          LOGD("Rejecting target due to ratio: %lf", ratio);
          rejected_targets.push_back(std::move(target));
          continue;
        }

      // Filter based on fullness
      //Fullness of boiler target is ~0.75 to ~1; fluctuates. Averages around 0.9
      const double kMinFullness = 0.7;
      const double kMaxFullness = 1.4;
      double original_contour_area = cv::contourArea(contour);
      double poly_area = cv::contourArea(poly);
      double fullness = original_contour_area / poly_area;
      if (fullness < kMinFullness || fullness > kMaxFullness) {
        LOGD("Rejected target due to fullness: %lf", fullness);
        rejected_targets.push_back(std::move(target));
        continue;
      }

      //Checks to find tallest target
      if (target.height > max_target_height){
        max_target_height = target.height;
      } else {
        rejected_targets.push_back(std::move(target));
        continue;
      }

      // We found a target
      //LOGD("Found target at %.2lf, %.2lf...size %.2lf, %.2lf", target.centroid_x, target.centroid_y, target.width, target.height);
      targets.push_back(std::move(target));
    }
  }


  //LOGD("Contour analysis costs %d ms", getTimeInterval(t));

  // write back
  t = getTimeMs();
  static cv::Mat vis;
  if (mode == DISP_MODE_RAW) {
    vis = input;
  } else if (mode == DISP_MODE_THRESH) {
    cv::cvtColor(thresh, vis, CV_GRAY2RGBA);
  } else {
    vis = input;
    // Render the targets
    for (auto &target : targets) {
      cv::polylines(vis, target.points, true, cv::Scalar(0, 112, 255), 3);
      cv::circle(vis, cv::Point(target.centroid_x, target.centroid_y), 5, cv::Scalar(0, 112, 255), 3);
    }
  }
  if (mode == DISP_MODE_TARGETS_PLUS) {
    for (auto &target : rejected_targets) {
      cv::polylines(vis, target.points, true, cv::Scalar(255, 0, 0), 3);
    }
  }
  //LOGD("Creating vis costs %d ms", getTimeInterval(t));

  glActiveTexture(GL_TEXTURE0);
  glBindTexture(GL_TEXTURE_2D, texOut);
  t = getTimeMs();
  glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE,
                  vis.data);
  //LOGD("glTexSubImage2D() costs %d ms", getTimeInterval(t));

  return targets;
}

static bool sFieldsRegistered = false;

static jfieldID sNumTargetsField;
static jfieldID sTargetsField;

static jfieldID sCentroidXField;
static jfieldID sCentroidYField;
static jfieldID sWidthField;
static jfieldID sHeightField;

static void ensureJniRegistered(JNIEnv *env) {
  if (sFieldsRegistered) {
    return;
  }
  sFieldsRegistered = true;
  jclass targetsInfoClass =
      env->FindClass("com/team254/cheezdroid/NativePart$TargetsInfo");
  sNumTargetsField = env->GetFieldID(targetsInfoClass, "numTargets", "I");
  sTargetsField = env->GetFieldID(
      targetsInfoClass, "targets",
      "[Lcom/team254/cheezdroid/NativePart$TargetsInfo$Target;");
  jclass targetClass =
      env->FindClass("com/team254/cheezdroid/NativePart$TargetsInfo$Target");

  sCentroidXField = env->GetFieldID(targetClass, "centroidX", "D");
  sCentroidYField = env->GetFieldID(targetClass, "centroidY", "D");
  sWidthField = env->GetFieldID(targetClass, "width", "D");
  sHeightField = env->GetFieldID(targetClass, "height", "D");
}

extern "C" void processFrame(JNIEnv *env, int tex1, int tex2, int w, int h,
                             int mode, int h_min, int h_max, int s_min,
                             int s_max, int v_min, int v_max,
                             jobject destTargetInfo) {
  auto targets = processImpl(w, h, tex2, static_cast<DisplayMode>(mode), h_min,
                             h_max, s_min, s_max, v_min, v_max);
  int numTargets = targets.size();
  ensureJniRegistered(env);
  env->SetIntField(destTargetInfo, sNumTargetsField, numTargets);
  if (numTargets == 0) {
    return;
  }
  jobjectArray targetsArray = static_cast<jobjectArray>(
      env->GetObjectField(destTargetInfo, sTargetsField));
  for (int i = 0; i < std::min(numTargets, 3); ++i) {
    jobject targetObject = env->GetObjectArrayElement(targetsArray, i);
    const auto &target = targets[i];
    env->SetDoubleField(targetObject, sCentroidXField, target.centroid_x);
    env->SetDoubleField(targetObject, sCentroidYField, target.centroid_y);
    env->SetDoubleField(targetObject, sWidthField, target.width);
    env->SetDoubleField(targetObject, sHeightField, target.height);
  }
}
