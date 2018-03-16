#pragma version(1)
#pragma rs_fp_relaxed
#pragma rs java_package_name(com.qhutch.elevationimageview)

uchar4 __attribute__((kernel)) root(uchar4 in, uint32_t x, uint32_t y) {
    float4 f4 = rsUnpackColor8888(in);

    float alpha = f4.a * 0.4f;

    return rsPackColorTo8888(0, 0, 0, alpha);
}