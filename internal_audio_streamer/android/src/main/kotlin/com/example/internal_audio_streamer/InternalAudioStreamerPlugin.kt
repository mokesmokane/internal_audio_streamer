package com.example.internal_audio_streamer

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.app.Activity
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File
import java.io.IOException
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import io.flutter.embedding.engine.plugins.FlutterPlugin
import android.content.Context
import android.content.BroadcastReceiver
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import android.annotation.TargetApi
import android.os.Build.VERSION_CODES
import io.flutter.plugin.common.EventChannel.EventSink
import android.content.IntentFilter
import android.content.Intent
import android.os.Build.VERSION
import android.content.ContextWrapper
import java.util.Locale
import android.provider.Settings
import androidx.annotation.RequiresApi
import kotlin.concurrent.thread
import com.service.MediaCaptureService

/** InternalAudioStreamerPlugin */
class InternalAudioStreamerPlugin :FlutterPlugin, ActivityAware, MethodCallHandler, EventChannel.StreamHandler, PluginRegistry.ActivityResultListener {


    private val SCREEN_RECORD_REQUEST_CODE = 333
    private val MEDIA_PROJECTION_REQUEST_CODE = 13
    private val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 42
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private  val LOG_TAG = "AudioCaptureService"
    private val SERVICE_ID = 123
    private val NOTIFICATION_CHANNEL_ID = "AudioCapture channel"

    private val NUM_SAMPLES_PER_READ = 1024
    private val BYTES_PER_SAMPLE = 2 // 2 bytes since we hardcoded the PCM 16-bit format
    private val BUFFER_SIZE_IN_BYTES = NUM_SAMPLES_PER_READ * BYTES_PER_SAMPLE
    
    private var mediaProjection: MediaProjection? = null

    private lateinit var audioCaptureThread: Thread
    private var audioRecord: AudioRecord? = null
    
    private var eventChannel: EventChannel? = null
    
    private var applicationContext: Context? = null
    private var activity: Activity? = null
    
    private var methodChannel: MethodChannel? = null


    private lateinit var channel : MethodChannel

    override fun onAttachedToEngine(binding: FlutterPluginBinding) {
        this.applicationContext = binding.applicationContext
        methodChannel = MethodChannel(binding.binaryMessenger, "internal_audio_streamer_commands")
        eventChannel = EventChannel(binding.binaryMessenger, "internal_audio_streamer")
        
        methodChannel!!.setMethodCallHandler(this)
        eventChannel!!.setStreamHandler(this)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
      this.activity = binding.activity
      binding.addActivityResultListener(this)
    }
    
    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
          
        println("onListen:MOKOOOOOOOKKKKKESS")
        MediaCaptureService.eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        
        println("onCancel:MOKOOOOOOOKKKKKESS")
        MediaCaptureService.eventSink = null
    }
    
    private fun isRelevantCode(code:Int):Boolean {
        return code == SCREEN_RECORD_REQUEST_CODE || code == MEDIA_PROJECTION_REQUEST_CODE || code == RECORD_AUDIO_PERMISSION_REQUEST_CODE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
      if(isRelevantCode(requestCode)!=true) return false
      println("onActivityResult:1")
      if(data==null) return false
      println("here we go: " + data!!.toString())
      val audioCaptureIntent = Intent(applicationContext!!, MediaCaptureService::class.java).apply {
          action = MediaCaptureService.ACTION_START
          putExtra(MediaCaptureService.EXTRA_RESULT_DATA, data!!)
      }
      println("onActivityResult:2")
      ContextCompat.startForegroundService(applicationContext!!, audioCaptureIntent)
      println("onActivityResult:3")
      return true
    }

    override fun onDetachedFromActivityForConfigChanges() {
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    }

    override fun onDetachedFromActivity() {
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        
        println("onDetachedFromEngine:MOKOOOOOOOKKKKKESS")
        applicationContext = null
        methodChannel!!.setMethodCallHandler(null)
        methodChannel = null
        eventChannel!!.setStreamHandler(null)
        eventChannel = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        
        println("onMethodCall:MOKOOOOOOOKKKKKESS")
        if (call.method == "getPlatformVersion") {
          result.success("Android ${android.os.Build.VERSION.RELEASE}")
        } 
        else if (call.method == "startRecordScreen") {
            try {
                startCapturing()
                result.success(true)

            } catch (e: Exception) {
                result.success(false)
            }
        } else if (call.method == "stopRecordScreen") {
            try {
              val audioCaptureIntent = Intent(applicationContext!!, MediaCaptureService::class.java).apply {
                action = MediaCaptureService.ACTION_STOP
            }
            println("onActivityResult:2")
            ContextCompat.startForegroundService(applicationContext!!, audioCaptureIntent)
                result.success("")
            } catch (e: Exception) {
                result.success("")
            }
        } else {
            result.notImplemented()
        }
    }

    // override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        
    //     println("onListen:MOKOOOOOOOKKKKKESS")
    //     eventSink = events
    // }

    // override fun onCancel(arguments: Any?) {
        
    //     println("onCancel:MOKOOOOOOOKKKKKESS")
    //     eventSink = null
    // }

    private fun startCapturing() {
        if (!isRecordAudioPermissionGranted()) {
          println("NO PErms")
          requestRecordAudioPermission()
        } else {
          println("START!!!")
          startMediaProjectionRequest()
        }
    }

    private fun isRecordAudioPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            applicationContext!!,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordAudioPermission() {
        println("requestRecordAudioPermission")
        if(activity != null){
          println("not null ")
          ActivityCompat.requestPermissions(
              activity!!,
              arrayOf(Manifest.permission.RECORD_AUDIO),
              RECORD_AUDIO_PERMISSION_REQUEST_CODE
          )
        }
    }
    

    fun startMediaProjectionRequest() {
        if(applicationContext!=null){
          mediaProjectionManager = applicationContext?.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
          println("HERE")
          ActivityCompat.startActivityForResult(activity!!, 
            mediaProjectionManager.createScreenCaptureIntent(),
            MEDIA_PROJECTION_REQUEST_CODE,
            null
          )
        }
    }

    //   private fun startAudioCapture() {
    //     println("HERE3")
    //     // TODO: add code for executing audio capture itself
    //     val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
    //         .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
    //         .build()

    //     val audioFormat = AudioFormat.Builder()
    //         .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
    //         .setSampleRate(8000)
    //         .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
    //         .build()

    //     audioRecord = AudioRecord.Builder()
    //         .setAudioFormat(audioFormat)
    //         .setBufferSizeInBytes(BUFFER_SIZE_IN_BYTES)
    //         .setAudioPlaybackCaptureConfig(config)
    //         .build()

    //     audioRecord!!.startRecording()
    //     println("YYEEEEEEEAAAAAHHHHHH")
    //     audioCaptureThread = thread(start = true) {
    //         writeAudioToFile()
    //     }
    // }

    // private fun writeAudioToFile() {
    //     println("HERE5")
    //     val capturedAudioSamples = ShortArray(NUM_SAMPLES_PER_READ)

    //     while (!audioCaptureThread.isInterrupted) {
    //         audioRecord?.read(capturedAudioSamples, 0, NUM_SAMPLES_PER_READ)
            
    //         eventSink!!.success(capturedAudioSamples)
    //     }
    // }


    private fun stopAudioCapture() {
        // TODO: Add code for stopping the audio capture
        requireNotNull(mediaProjection) { "Tried to stop audio capture, but there was no ongoing capture in place!" }

        audioCaptureThread.interrupt()
        audioCaptureThread.join()

        audioRecord!!.stop()
        audioRecord!!.release()
        audioRecord = null

        mediaProjection!!.stop()
        // stopSelf()
    }

}


