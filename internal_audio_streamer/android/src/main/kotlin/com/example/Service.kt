 package com.service
 import android.app.Activity
 import android.app.NotificationChannel
 import android.app.NotificationManager
 import android.app.Service
 import android.content.Context
 import android.content.Intent
 import android.icu.text.SimpleDateFormat
 import android.media.AudioAttributes
 import android.media.AudioFormat
 import android.media.AudioPlaybackCaptureConfiguration
 import android.media.AudioRecord
 import android.media.projection.MediaProjection
 import android.media.projection.MediaProjectionManager
 import android.os.IBinder
 import android.os.Looper
 import android.os.Handler
 import android.util.Log
 import java.io.File
 import java.io.FileOutputStream
 import java.util.*
 import kotlin.concurrent.thread
 import kotlin.experimental.and
 import android.app.PendingIntent
 import android.os.Build
 import androidx.core.app.NotificationCompat
 import androidx.core.content.ContextCompat
 import io.flutter.plugin.common.EventChannel
 
 class MediaCaptureService : Service() {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null

    private lateinit var audioCaptureThread: Thread
    private var audioRecord: AudioRecord? = null
     
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("onSTartCOmand")
        super.onCreate()
        
        createNotificationChannel()

        startForeground(SERVICE_ID, NotificationCompat.Builder(this,
                NOTIFICATION_CHANNEL_ID).build())
        
        
        mediaProjectionManager = applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        
        return if (intent != null) {
            when (intent.action) {
                ACTION_START -> {
                    println("X1")
                    val maybemediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, intent.getParcelableExtra(EXTRA_RESULT_DATA)!!)
                    if (maybemediaProjection != null) {
                        mediaProjection = maybemediaProjection as MediaProjection
                        println("X2")
                        startAudioCapture()
                        Service.START_STICKY
                    }
                    else{
                        println("X3")
                        Service.START_NOT_STICKY
                    }
                }
                ACTION_STOP -> {
                    stopAudioCapture()
                    Service.START_NOT_STICKY
                }
                else -> throw IllegalArgumentException("Unexpected action received: ${intent.action}")
            }
        } else {
            Service.START_NOT_STICKY
        }
    }

    private fun createNotificationChannel() {
        println("HERE2")
        val serviceChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID,
                "Audio Capture Service Channel", NotificationManager.IMPORTANCE_DEFAULT)

        val manager = getSystemService(NotificationManager::class.java) as NotificationManager
        manager.createNotificationChannel(serviceChannel)
    }

    private fun startAudioCapture() {
        println("HERE3")
        // TODO: add code for executing audio capture itself
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(8000)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()

        audioRecord = AudioRecord.Builder()
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(BUFFER_SIZE_IN_BYTES)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        audioRecord!!.startRecording()
        println("YYEEEEEEEAAAAAHHHHHH")
        audioCaptureThread = thread(start = true) {
        //  val outputFile = createAudioFile()
        //  Log.d(LOG_TAG, "Created file for capture target: ${outputFile.absolutePath}")
            writeAudioToFile()
        }
    }

    private fun createAudioFile(): File {
        println("HERE4")
        val audioCapturesDirectory = File(getExternalFilesDir(null), "/AudioCaptures")
        if (!audioCapturesDirectory.exists()) {
            audioCapturesDirectory.mkdirs()
        }
        val timestamp = SimpleDateFormat("dd-MM-yyyy-hh-mm-ss", Locale.US).format(Date())
        val fileName = "Capture-$timestamp.pcm"
        return File(audioCapturesDirectory.absolutePath + "/" + fileName)
    }
 
    private fun writeAudioToFile() {
        println("HERE5")
        // val fileOutputStream = FileOutputStream(outputFile)
        val capturedAudioSamples = ShortArray(NUM_SAMPLES_PER_READ)
 
        while (!audioCaptureThread.isInterrupted) {
            audioRecord?.read(capturedAudioSamples, 0, NUM_SAMPLES_PER_READ)
            // println(capturedAudioSamples.toByteArray())
            Handler(Looper.getMainLooper()).post {
                if(eventSink!=null){
                    eventSink!!.success(capturedAudioSamples.toByteArray())
                }
                else{
                    stopAudioCapture()
                }
            }
            
        }
    }
 
    private fun stopAudioCapture() {
        // TODO: Add code for stopping the audio capture
        requireNotNull(mediaProjection) { "Tried to stop audio capture, but there was no ongoing capture in place!" }

        audioCaptureThread.interrupt()
        audioCaptureThread.join()

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    
        mediaProjection?.stop()
        println("stoooooooop")
        stopForeground(true)
        stopSelf()
    }

    override fun onBind(p0: Intent?): IBinder? = null

    private fun ShortArray.toByteArray(): ByteArray {
        val bytes = ByteArray(size * 2)
        for (i in 0 until size) {
            bytes[i * 2] = (this[i] and 0x00FF).toByte()
            bytes[i * 2 + 1] = (this[i].toInt() shr 8).toByte()
            this[i] = 0
        }
        return bytes
    }

    companion object {
        
        var eventSink: EventChannel.EventSink? = null
        private const val LOG_TAG = "AudioCaptureService"
        private const val SERVICE_ID = 123
        private const val NOTIFICATION_CHANNEL_ID = "AudioCapture channel"

        private const val NUM_SAMPLES_PER_READ = 1024
        private const val BYTES_PER_SAMPLE = 2 // 2 bytes since we hardcoded the PCM 16-bit format
        private const val BUFFER_SIZE_IN_BYTES = NUM_SAMPLES_PER_READ * BYTES_PER_SAMPLE

        const val ACTION_START = "AudioCaptureService:Start"
        const val ACTION_STOP = "AudioCaptureService:Stop"
        const val EXTRA_RESULT_DATA = "AudioCaptureService:Extra:ResultData"
    }
}
