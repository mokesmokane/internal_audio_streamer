import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'internal_audio_streamer_platform_interface.dart';

/// An implementation of [InternalAudioStreamerPlatform] that uses method channels.
class MethodChannelInternalAudioStreamer extends InternalAudioStreamerPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('internal_audio_streamer_commands');
  @visibleForTesting
  final eventChannel = const EventChannel('internal_audio_streamer');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  
  Stream<Uint8List>? _audioStream;

  @override
  Future<void> startRecording() => methodChannel
      .invokeMethod('startRecordScreen');

  @override
  Future<void> stopRecording() => methodChannel
      .invokeMethod('stopRecordScreen');

  @override
  Stream<Uint8List> get onAudio {
    _audioStream ??= eventChannel
        .receiveBroadcastStream()
        .map((dynamic event) => event as Uint8List);
    return _audioStream!;
  }
}
