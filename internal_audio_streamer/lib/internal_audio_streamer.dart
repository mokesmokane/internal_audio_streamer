
import 'dart:typed_data';

import 'internal_audio_streamer_platform_interface.dart';

class InternalAudioStreamer {
  Future<String?> getPlatformVersion() {
    return InternalAudioStreamerPlatform.instance.getPlatformVersion();
  }

  Future<void> start() {
    return InternalAudioStreamerPlatform.instance.startRecording();
  }

  Future<void> stop() {
    return InternalAudioStreamerPlatform.instance.stopRecording();
  }

  Stream<Uint8List> get onAudio {
    return InternalAudioStreamerPlatform.instance.onAudio;
  }
}
