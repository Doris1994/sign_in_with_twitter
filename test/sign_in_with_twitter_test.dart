import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
//import 'package:sign_in_with_twitter/sign_in_with_twitter.dart';

void main() {
  const MethodChannel channel = MethodChannel('sign_in_with_twitter');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  // test('getPlatformVersion', () async {
  //   expect(await SignInWithTwitter.platformVersion, '42');
  // });
}
