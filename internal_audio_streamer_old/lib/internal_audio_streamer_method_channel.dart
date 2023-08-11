import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'internal_audio_streamer_platform_interface.dart';

/// An implementation of [InternalAudioStreamerPlatform] that uses method channels.
class MethodChannelInternalAudioStreamer extends InternalAudioStreamerPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('internal_audio_streamer_commands');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
