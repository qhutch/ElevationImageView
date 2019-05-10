package com.qhutch.shadowimageview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                setImageElevation(p1)
            }
        })
    }

    private fun setImageElevation(elevation: Int) {
        textView.text = "Elevation: $elevation dp"
        imageview1.setElevationDp(elevation.toFloat())
        imageview2.setElevationDp(elevation.toFloat())
        imageview3.setElevationDp(elevation.toFloat())
        imageview4.setElevationDp(elevation.toFloat())
    }
}
