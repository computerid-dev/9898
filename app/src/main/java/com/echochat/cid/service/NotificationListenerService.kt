package com.echochat.cid.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.echochat.cid.R
import com.echochat.cid.data.AppDatabase
import com.echochat.cid.data.FirestoreRepository
import com.echochat.cid.data.Message
import com.echochat.cid.ui.ChatActivity
import com.echochat.cid.ui.MainActivity
import com.echochat.cid.util.SessionManager
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Foreground service yang tetap hidup di latar belakang (mirip pemutar musik/VPN),
 * mendengarkan Firestore untuk semua pesan masuk, dan menampilkan notifikasi lokal.
 * Berhenti sendiri kalau HP di-restart; user perlu buka app lagi untuk mengaktifkan ulang.
 */
class NotificationListenerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var session: SessionManager
    private val firestoreRepository = FirestoreRepository()
    private var listener: ListenerRegistration? = null

    override fun onCreate() {
        super.onCreate()
        session = SessionManager(this)
        createNotificationChannel()
        startForeground(FOREGROUND_ID, buildForegroundNotification())
        listenIncoming()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun listenIncoming() {
        val myUid = session.myUid
        listener = firestoreRepository.listenAllIncomingMessages(myUid) { chatId, remoteMessage ->
            if (remoteMessage.senderUid == myUid) return@listenAllIncomingMessages
            if (remoteMessage.timestampMillis <= session.lastNotifiedTimestamp) return@listenAllIncomingMessages
            if (ChatActivity.currentOpenChatUid == remoteMessage.senderUid) return@listenAllIncomingMessages

            serviceScope.launch {
                val database = AppDatabase.getInstance(applicationContext)
                val messageDao = database.messageDao()
                val existing = messageDao.findByRemoteId(remoteMessage.remoteId)
                if (existing == null) {
                    try {
                        messageDao.insert(
                            Message(
                                chatWithUid = remoteMessage.senderUid,
                                content = remoteMessage.content,
                                isMine = false,
                                remoteId = remoteMessage.remoteId,
                                isRead = false,
                                timestamp = remoteMessage.timestampMillis
                            )
                        )
                        val friend = database.friendDao().findByUid(remoteMessage.senderUid)
                        showMessageNotification(
                            senderUid = remoteMessage.senderUid,
                            senderNickname = friend?.nickname ?: remoteMessage.senderUid,
                            content = remoteMessage.content
                        )
                    } catch (error: android.database.sqlite.SQLiteConstraintException) {
                        // Sudah keburu dimasukkan oleh layar chat yang sedang terbuka - aman diabaikan.
                    }
                }
                session.lastNotifiedTimestamp = remoteMessage.timestampMillis
            }
        }
    }

    private fun showMessageNotification(senderUid: String, senderNickname: String, content: String) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra(ChatActivity.EXTRA_FRIEND_UID, senderUid)
            putExtra(ChatActivity.EXTRA_FRIEND_NICKNAME, senderNickname)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, senderUid.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, MESSAGE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_send)
            .setContentTitle(senderNickname)
            .setContentText(content)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(senderUid.hashCode(), notification)
    }

    private fun buildForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_send)
            .setContentTitle(getString(R.string.notification_service_title))
            .setContentText(getString(R.string.notification_service_desc))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val serviceChannel = NotificationChannel(
            SERVICE_CHANNEL_ID, getString(R.string.notification_service_title), NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(serviceChannel)

        val messageChannel = NotificationChannel(
            MESSAGE_CHANNEL_ID, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(messageChannel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        listener?.remove()
        serviceScope.launch { }.let { serviceScope.coroutineContext[Job]?.cancel() }
    }

    companion object {
        private const val FOREGROUND_ID = 1001
        private const val SERVICE_CHANNEL_ID = "echochat_service_channel"
        private const val MESSAGE_CHANNEL_ID = "echochat_message_channel"

        fun start(context: Context) {
            val intent = Intent(context, NotificationListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NotificationListenerService::class.java))
        }
    }
}
