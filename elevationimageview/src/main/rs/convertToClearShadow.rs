#pragma version(1)
#pragma rs_fp_relaxed
#pragma rs java_package_name(com.qhutch.elevationimageview)

bool isTranslucent = false;

uchar4 __attribute__((kernel)) root(uchar4 in, uint32_t x, uint32_t y) {
    float4 f4 = rsUnpackColor8888(in);

    if (isTranslucent) {
        return rsPackColorTo8888(0.4f * f4.r, 0.4f * f4.g, 0.4f * f4.b, 0.6f * f4.a);
    } else {
        return rsPackColorTo8888(0, 0, 0, 0.4f * f4.a);
    }
}