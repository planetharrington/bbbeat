package com.shaunharrington.bbbeat;

import java.nio.ShortBuffer;

/**
 * Create a new Metronome object to generate continuous PCM data with a
 * metronome "click track". Each measure will be indicated with a measure click
 * and each of the remaining beats in the measure will be indicated with a beat
 * tick.
 * 
 * @param sampleRate
 *            The sample rate at which the PCM data should be generated.
 * 
 * @param tempo
 *            The beats per minute tempo for the metronome.
 * 
 * @param beatsPerMeasure
 *            Number of beats per measure. Should probably be in the range of 2
 *            to about 8. In the music notation time signature, this is the top
 *            number.
 */
public class metronome {

	/**
	 * Create a new Metronome object to generate continuous PCM data with a
	 * metronome "click track". Each measure will be indicated with a measure
	 * click and each of the remaining beats in the measure will be indicated
	 * with a beat tick.
	 * 
	 * @param sampleRate
	 *            The sample rate at which the PCM data should be generated.
	 * 
	 * @param tempo
	 *            The beats per minute tempo for the metronome.
	 * 
	 * @param beatsPerMeasure
	 *            Number of beats per measure. Should probably be in the range
	 *            of 2 to about 8. In the music notation time signature, this is
	 *            the top number.
	 */
	public metronome(int sampleRate, int tempo, int beatsPM, boolean bAsscending) {
		super();
		beatIncrement = (float) BEAT_TONE_FREQUENCY / (float) sampleRate;
		measureIncrement = (float) MEASURE_TONE_FREQUENCY / (float) sampleRate;
		samplesPerBeat = 60.0f * (float) sampleRate / (float) tempo;
		samplesPerMeasure = samplesPerBeat * beatsPerMeasure;
		tickLength = (float) TICK_DURATION_MS * (float) sampleRate / 1000.0f;
		state = 0;
		toneCounter = 0.0f;
		sampleValue = 1;
		measureSampleCounter = 0;
		beatSampleCounter = 0;
		beatsPerMeasure = beatsPM;
		beatCounter = 1;
		m_bAsscending = bAsscending;
	}

	// Frequencies of the two different tones. 2000 and 1000 Hz values will
	// give the original "Pong" feels...
	private int MEASURE_TONE_FREQUENCY = 2000;

	private int BEAT_TONE_FREQUENCY = 1000;

	// Duration of the ticks in milliseconds
	private int TICK_DURATION_MS = 20;

	// The tick volume is a number between 0 and 32767
	private short TICK_VOLUME = 20000;

	/**
	 * Fill a buffer with the next set of samples that will make up the
	 * metronome click track.
	 * 
	 * @param buffer
	 *            The array of shorts where the PCM data will be placed.
	 * 
	 * @param length
	 *            The number of PCM samples to put into the buffer.
	 */
	void fillBuffer(ShortBuffer shortBuffer, int length) {
		for (int index = 0; index < length; index++) {
			switch (state) {
			case 0:// GENERATING_MEASURE_TONE
				toneCounter += m_bAsscending ? beatIncrement : measureIncrement;
				if (toneCounter > 1.0f) {
					toneCounter -= (int) toneCounter;
					sampleValue *= -1;
				}
				if (measureSampleCounter > tickLength) {
					state = 2;
					beatCounter = 1;
					sampleValue = 0;
				}
				break;

			case 1:// GENERATING_BEAT_TONE:
				toneCounter += m_bAsscending ? measureIncrement : beatIncrement;
				if (toneCounter > 1.0f) {
					toneCounter -= (int) toneCounter;
					sampleValue *= -1;
				}
				if (beatSampleCounter > tickLength) {
					state = 2;
					sampleValue = 0;
				}
				break;

			case 2:// GENERATING_SILENCE:
				//
				// Figure out if we're at a beat
				//
				if (beatSampleCounter > samplesPerBeat) {
					if (beatCounter < beatsPerMeasure) {
						sampleValue = 1;
						state = 1;
					} else {
						state = 0;
						beatCounter = 0;
						sampleValue = 1;
						measureSampleCounter = 0;
					}
					beatCounter++;
					beatSampleCounter = 0;
				}
				break;
			}
			measureSampleCounter++;
			beatSampleCounter++;
			short sample = (short) (((short) sampleValue) * ((short) TICK_VOLUME));
			shortBuffer.put(index, (short) sample);
		}
	}

	private float samplesPerMeasure;

	private float samplesPerBeat;

	private float tickLength;

	private float measureIncrement;

	private float beatIncrement;

	private int state;

	private int beatCounter;

	private int beatsPerMeasure;

	private short sampleValue;

	private float toneCounter;

	private int measureSampleCounter;

	private int beatSampleCounter;

	private boolean m_bAsscending;
}
