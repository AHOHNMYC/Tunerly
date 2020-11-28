package com.example.tuner

import kotlin.math.round
import kotlin.math.log2
import kotlin.math.abs
import kotlin.math.pow

class PitchProcessing(callback: MyCallback?) {
    private var myCallback: MyCallback? = callback
    private val allNotes = arrayOf("A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#")
    private val pitchMap = mapOf(16.35F to "C", 17.32F to "C#", 18.35F to "D", 19.45F to "D#",
        20.60F to "E", 21.83F to "F", 23.12F to "F#", 24.50F to "G", 25.96F to "G#", 27.50F to "A",
        29.14F to "A#", 30.87F to "B", 32.70F to "C", 34.65F to "C#", 36.71F to "D", 38.90F to "D#",
        41.20F to "E", 43.65F to "F", 46.25F to "F#", 49.00F to "G", 51.91F to "G#", 55.00F to "A",
        58.27F to "A#", 61.74F to "B", 65.40F to "C", 69.30F to "Db", 73.42F to "D", 77.78F to "Eb",
        82.41F to "E", 87.31F to "F", 92.50F to "F#", 98.00F to "G", 103.83F to "Ab", 110.00F to "A",
        116.54F to "A#", 123.47F to "B", 130.81F to "C", 138.59F to "Db", 146.83F to "D",
        155.56F to "D#", 164.81F to "E", 174.61F to "F", 185.00F to "Gb", 196.00F to "G",
        207.65F to "G#", 220.00F to "A", 233.08F to "Bb", 246.94F to "B", 261.63F to "C",
        277.18F to "C#", 293.66F to "D", 311.13F to "Eb", 329.63F to "E", 349.23F to "F",
        370.00F to "Gb", 392.00F to "G", 415.30F to "G#", 440.00F to "A", 466.16F to "A#",
        493.88F to "B", 523.25F to "C", 554.37F to "C#", 587.33F to "D", 622.25F to "D#",
        659.26F to "E", 698.46F to "F", 739.99F to "F#", 784.00F to "G", 830.61F to "G#",
        880.00F to "A", 932.33F to "A#", 987.76F to "B", 1046.50F to "C", 1108.73F to "C#",
        1174.66F to "D", 1244.50F to "D#", 1318.51F to "E", 1396.91F to "F", 1479.98F to "F#",
        1567.98F to "G", 1661.22F to "G#", 1760.00F to "A", 1864.66F to "A#", 1975.53F to "B",
        2093.01F to "C", 2217.46F to "C#", 2349.32F to "D", 2489.02F to "D#", 2637.02F to "E",
        2793.83F to "F", 2959.96F to "F#", 3135.96F to "G", 3322.44F to "G#", 3520.00F to "A",
        3729.31F to "A#", 3951.07F to "B", 4186.01F to "C")

    private val tuningMap = mapOf("Standard Tuning" to TuningData.GuitarStandard,
        "E-Flat" to TuningData.GuitarEFlat, "Drop D" to TuningData.GuitarDropD,
        "Drop Db" to TuningData.GuitarDropDb, "Double Drop D" to TuningData.GuitarDoubleDropD,
        "DADGAD" to TuningData.GuitarDADGAD,
        "Open D" to TuningData.GuitarOpenD,
        "Open A" to TuningData.GuitarOpenA,
        "Open E" to TuningData.GuitarOpenE,
        "Open G" to TuningData.GuitarOpenG,
        "4 String Standard" to TuningData.BassStandard4,
        "5 String Standard" to TuningData.BassStandard5,
        "6 String Standard" to TuningData.BassStandard6,
        "4 String Drop D" to TuningData.BassDropD,
        "4 String D Standard" to TuningData.BassDStandard,
        "4 String Drop C" to TuningData.BassDropC,
        "Standard Reentrant" to TuningData.UkuleleStandard,
        "Traditional Soprano" to TuningData.UkuleleTraditional,
        "Baritone" to TuningData.UkuleleBaritone,
        "Bass" to TuningData.UkuleleBass)

    fun tune(pitchInHz: Float, probability: Float) {
        if (MainActivity.CurInstrument.curInstrument == "Chromatic") {
            closestNote(pitchInHz)
            if (probability > 0.92) {
                myCallback?.updateNote(closestNote(pitchInHz))
                tuningDirection(pitchInHz, closestPitch(pitchInHz))
            }
        } else {
            // stores pitches in Hz of current selected tuning
            val pitches = (tuningMap[MainActivity.CurTuning.curTuning] ?: error("")).frequencies
            (myCallback as MainActivity).noteSize()
            if (tuningMap.keys.contains(MainActivity.CurTuning.curTuning)) {
                val stringPitch = closestString(pitchInHz, pitches)
                if (probability > 0.92) {
                    myCallback?.updateNote(pitchMap.getValue(stringPitch))
                    tuningDirection(pitchInHz, stringPitch)
                }
            }
        }
    }

    private fun tuningDirection(curPitch : Float, toPitch : Float) {
        var tuneUp = true
        var tuneDown = true
        // calculate interval in cents from hz given
        val cents : Float = 1200 * log2(toPitch / curPitch)
        if (cents > 10 || cents < -10) {
            if (cents > 10) {
                tuneUp = true
                tuneDown = false
            }
            else if (cents < -10) {
                tuneUp = false
                tuneDown = true
            }
        }
        else {
            tuneUp = false
            tuneDown = false
        }

        if (!tuneUp && !tuneDown) {
            myCallback?.colorTuned("none")
        }
        else if (tuneUp && !tuneDown) {
            myCallback?.colorTuned("up")
        }
        else if (!tuneUp && tuneDown){
            myCallback?.colorTuned("down")
        }
    }

    /**
     * determine which string current note is closest to
     * @return Float (Frequency in Hz)
     */
    private fun closestString(pitchInHz: Float, tuning: FloatArray) : Float {
        var difference = abs(tuning[0] - pitchInHz)
        var idx = 0
        for (c in 1 until tuning.size) {
            val cdifference = abs(tuning[c] - pitchInHz)
            if (cdifference < difference) {
                idx = c
                difference = cdifference
            }
        }
        return tuning[idx]
    }

    /** detects closest note in A = 440 with equal temperament formula:
     * pitch(i) = pitch(0) * 2^(halfSteps/12)
     * therefore formula to derive interval between two pitches:
     * i = 12 * log2 * (pitch(i)/pitch(o))
     */
    private fun closestNote(pitchInHz: Float) : String {
        (myCallback as MainActivity).noteSize()
        val i = (round(log2(pitchInHz / 440) * 12)).toInt()
        // floorMod implementation to prevent ArrayIndexOutOfBoundException
        return allNotes[(i % 12 + 12) % 12]
    }

    private fun closestPitch(pitchInHz: Float): Float {
        val i = (round(log2(pitchInHz / 440) * 12)).toInt()
        val closestPitch = 440 * 2.toDouble().pow(i.toDouble() / 12)
        return closestPitch.toFloat()
    }
}

