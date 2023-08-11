import 'package:flutter_test/flutter_test.dart';
import 'package:internal_audio_streamer/internal_audio_streamer.dart';
import 'package:internal_audio_streamer/internal_audio_streamer_platform_interface.dart';
import 'package:internal_audio_streamer/internal_audio_streamer_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockInternalAudioStreamerPlatform
    with MockPlatformInterfaceMixin
    implements InternalAudioStreamerPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final InternalAudioStreamerPlatform initialPlatform = InternalAudioStreamerPlatform.instance;

  test('$MethodChannelInternalAudioStreamer is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelInternalAudioStreamer>());
  });

  test('getPlatformVersion', () async {
    InternalAudioStreamer internalAudioStreamerPlugin = InternalAudioStreamer();
    MockInternalAudioStreamerPlatform fakePlatform = MockInternalAudioStreamerPlatform();
    InternalAudioStreamerPlatform.instance = fakePlatform;

    expect(await internalAudioStreamerPlugin.getPlatformVersion(), '42');
  });
}
