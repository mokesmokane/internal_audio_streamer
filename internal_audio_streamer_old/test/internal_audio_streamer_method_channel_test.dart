import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:internal_audio_streamer/internal_audio_streamer_method_channel.dart';

void main() {
  MethodChannelInternalAudioStreamer platform = MethodChannelInternalAudioStreamer();
  const MethodChannel channel = MethodChannel('internal_audio_streamer');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
