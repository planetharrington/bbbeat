package com.shaunharrington.bbbeat;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JSpinner;

public class BBBeat extends JFrame implements WindowListener {

	private JPanel mToolbar;

	private JPanel mToolbar2;

	private JButton playButton;

	private boolean boolPaused = false;

	AudioFormat audioFormat;

	SourceDataLine sourceDataLine;

	PlayThread playThread = null;

	private metronome mMet;

	int LOWEST_BEAT_TEMPO = 30;

	int HIGHEST_BEAT_TEMPO = 320;

	int beatTempo = 120;

	int beatPattern = 2; // = "4/4"

	String[] patterns = { "2/4", "3/4", "4/4", "5/8", "6/8", "7/8" };

	private JComboBox comboPattern = new JComboBox(patterns);

	SpinnerNumberModel model = new SpinnerNumberModel(beatTempo,
			LOWEST_BEAT_TEMPO, HIGHEST_BEAT_TEMPO, 1);

	JSpinner jSpinner = new JSpinner(model);

	NumberEditor textField1 = new NumberEditor(jSpinner);

	public BBBeat() throws Exception {
		super("BBBeat by Shaun Harrington");
		initComponents();
	}

	private void initComponents() throws Exception {
		addWindowListener(this);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setBounds((screenSize.width - 280) / 2, ((screenSize.height - 90) / 2)
				- (90 * 2), 280, 90);

		mMet = new metronome(44100, beatTempo, beatPattern, true);

		GridBagConstraints gridBagConstraints;
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;

		// Build the tool bar... the play/stop button makes up the rest of the
		// pane.
		mToolbar = new JPanel();
		mToolbar.setLayout(new GridBagLayout());
		gridBagConstraints.gridx = -1;
		mToolbar.add(jSpinner, gridBagConstraints);// , gridBagConstraints);
		gridBagConstraints.gridx = 1;
		mToolbar.add(comboPattern, gridBagConstraints);
		comboPattern.setSelectedItem(new Integer(beatPattern));
		comboPattern.setToolTipText("Select the pattern."); //$NON-NLS-1$
		comboPattern.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				patternActionPerformed(e);
			}
		});

		jSpinner.setToolTipText("Enter the exact tempo value.");
		model.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				Object source = e.getSource();
				beatTempo = ((Number) model.getValue()).intValue();
				String stringVal = comboPattern.getSelectedItem().toString();
				try {
					beatPattern = Integer.parseInt(stringVal);
				} catch (Exception exc) {
					;
				}
				mMet = new metronome(44100, beatTempo, beatPattern, true);
			};
		});
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(mToolbar, BorderLayout.NORTH);

		playButton = new JButton();
		mToolbar2 = new JPanel();
		mToolbar2.setLayout(new BorderLayout());
		mToolbar2.setMinimumSize(new Dimension(10, 50));
		playButton.setText("Play"); //$NON-NLS-1$
		playButton.setToolTipText("Play the indicated beat."); //$NON-NLS-1$
		playButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				playActionPerformed(e);
			}
		});
		mToolbar2.add(playButton);
		getContentPane().add(mToolbar2, BorderLayout.CENTER);
	}

	protected void patternActionPerformed(ActionEvent e) {
		Object source = e.getSource();
		beatPattern = comboPattern.getSelectedIndex() + 2;
		mMet = new metronome(44100, beatTempo, beatPattern, true);
	}

	private boolean playAudio() {
		boolean boolRet = true;
		beatTempo = ((Number) model.getValue()).intValue();
		try {
			if (playThread != null && playThread.isAlive()) {
				boolRet = false;
				playThread.stop = true;
			} else {
				// Get things set up for capture
				audioFormat = getAudioFormat();
				DataLine.Info dataLineInfo = new DataLine.Info(
						SourceDataLine.class, audioFormat);
				sourceDataLine = (SourceDataLine) AudioSystem
						.getLine(dataLineInfo);
				playThread = new PlayThread();
				playThread.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}// end catch
		return boolRet;
	}// end captureAudio method

	private AudioFormat getAudioFormat() {
		float sampleRate = 44100.0F;
		int sampleSizeInBits = 16;
		int channels = 1;
		boolean signed = true;
		boolean bigEndian = false;
		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed,
				bigEndian);
	}// end getAudioFormat

	protected void playActionPerformed(ActionEvent e) {
		if (playAudio())
			((JButton) e.getSource()).setText("Stop"); //$NON-NLS-1$
		else
			((JButton) e.getSource()).setText("Play"); //$NON-NLS-1$
	}

	class PlayThread extends Thread {
		public boolean stop = false;

		public void run() {
			try {
				sourceDataLine.open(audioFormat);
				sourceDataLine.start();

				ByteBuffer byteBuffer = ByteBuffer.allocate(4000);
				byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

				while (!stop) {
					byteBuffer.rewind();
					FillBuffer(byteBuffer.asShortBuffer(), 2000);
					byteBuffer.rewind();
					sourceDataLine.write(byteBuffer.array(), 0, 4000);
				}
				sourceDataLine.stop();
				sourceDataLine.close();
			} catch (Exception e) {
				e.printStackTrace();
			}// end catch
		}// end run

		void FillBuffer(ShortBuffer shortBuffer, int dwLength) {
			if (mMet != null)
				mMet.fillBuffer(shortBuffer, dwLength);
		}
	}

	public void windowActivated(WindowEvent e) {
	}

	public void windowClosing(WindowEvent e) {
	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowOpened(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
		if (playThread != null && playThread.isAlive()) {
			playThread.stop = true;
			boolPaused = true;
		}
	}

	public void windowDeiconified(WindowEvent e) {
		if (boolPaused) {
			boolPaused = false;
			playAudio();
		}
	}

	public void windowDeactivated(WindowEvent e) {
	}

	public void windowGainedFocus(WindowEvent e) {
	}

	public void windowLostFocus(WindowEvent e) {
	}

	public void windowStateChanged(WindowEvent e) {
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable e1) {
			e1.printStackTrace();
		}
		EventQueue.invokeLater(new Runnable() {

			public void run() {
				try {
					new BBBeat().setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

}
