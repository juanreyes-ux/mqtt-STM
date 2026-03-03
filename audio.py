import wave, contextlib

wav_path = r"C:\Users\ABS 205\Downloads\FCS_OK_1b_4ch.wav"

with contextlib.closing(wave.open(wav_path, 'r')) as wf:
    frames = wf.getnframes()
    rate = wf.getframerate()
    duration_ms = int(round(frames / float(rate) * 1000))

print(duration_ms)
