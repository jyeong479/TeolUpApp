package com.example.teolupapp.fragments

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.teolupapp.LoginActivity
import com.example.teolupapp.R
import com.example.teolupapp.models.Refri
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class SettingFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private val CHANNEL_ID = "expiry_notification_channel"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            checkExpiryAndNotify()
        } else {
            Toast.makeText(context, "알림 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            view?.findViewById<Switch>(R.id.switch1)?.isChecked = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        createNotificationChannel()

        val profileManageBtn = view.findViewById<LinearLayout>(R.id.btn_profile_manage)
        val logoutBtn = view.findViewById<LinearLayout>(R.id.btn_logout)
        val deleteAccountBtn = view.findViewById<LinearLayout>(R.id.btn_delete_account)
        val notiSwitch = view.findViewById<Switch>(R.id.switch1)

        profileManageBtn.setOnClickListener {
            showProfileEditDialog()
        }

        notiSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkPermissionAndNotify()
            }
        }

        logoutBtn.setOnClickListener {
            showLogoutDialog()
        }

        deleteAccountBtn.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun showProfileEditDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_profile_edit, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val editNickname = dialogView.findViewById<TextInputEditText>(R.id.edit_nickname)
        val editPassword = dialogView.findViewById<TextInputEditText>(R.id.edit_password)
        val editPasswordConfirm = dialogView.findViewById<TextInputEditText>(R.id.edit_password_confirm)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_save)

        val user = auth.currentUser
        editNickname.setText(user?.displayName)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val newNickname = editNickname.text.toString().trim()
            val newPassword = editPassword.text.toString().trim()
            val confirmPassword = editPasswordConfirm.text.toString().trim()

            if (newNickname.isEmpty()) {
                Toast.makeText(context, "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 비밀번호 변경 시도 시 확인 절차
            if (newPassword.isNotEmpty()) {
                if (newPassword != confirmPassword) {
                    Toast.makeText(context, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (newPassword.length < 6) {
                    Toast.makeText(context, "비밀번호는 6자리 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // 1. 닉네임 업데이트
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newNickname)
                .build()

            user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 2. 비밀번호 변경 (입력된 경우에만)
                    if (newPassword.isNotEmpty()) {
                        user.updatePassword(newPassword).addOnCompleteListener { pwTask ->
                            if (pwTask.isSuccessful) {
                                Toast.makeText(context, "프로필과 비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            } else {
                                Toast.makeText(context, "비밀번호 변경 실패: ${pwTask.exception?.message}\n재로그인이 필요할 수 있습니다.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "프로필이 변경되었습니다.", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                } else {
                    Toast.makeText(context, "프로필 업데이트 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun checkPermissionAndNotify() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                checkExpiryAndNotify()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            checkExpiryAndNotify()
        }
    }

    private fun checkExpiryAndNotify() {
        val user = auth.currentUser ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("refri").child(user.uid)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                var notificationCount = 0
                val today = LocalDate.now()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

                for (data in snapshot.children) {
                    val refri = data.getValue(Refri::class.java)
                    if (refri != null && refri.expiryDate.isNotEmpty()) {
                        try {
                            val expiryDate = LocalDate.parse(refri.expiryDate, formatter)
                            val daysUntil = ChronoUnit.DAYS.between(today, expiryDate)

                            if (daysUntil <= 7) {
                                sendNotification(refri.name, daysUntil)
                                notificationCount++
                            }
                        } catch (e: Exception) {
                        }
                    }
                }

                if (notificationCount == 0) {
                    Toast.makeText(context, "임박한 재료가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "데이터 로드 실패", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendNotification(name: String, daysUntil: Long) {
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val title = "유통기한 임박 알림"
        val message = when {
            daysUntil < 0 -> "${name}의 유통기한이 지났습니다! (${-daysUntil}일 지남)"
            daysUntil == 0L -> "${name}의 유통기한이 오늘까지입니다!"
            else -> "${name}의 유통기한이 ${daysUntil}일 남았습니다."
        }

        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "유통기한 알림"
            val descriptionText = "유통기한 임박 재료 알림 채널"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("로그아웃")
            .setMessage("정말 로그아웃 하시겠습니까?")
            .setPositiveButton("로그아웃") { _, _ ->
                auth.signOut()
                moveToLogin()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("회원탈퇴")
            .setMessage("정말 탈퇴하시겠습니까?\n저장된 냉장고 재료와 장바구니 목록이 모두 삭제됩니다.")
            .setPositiveButton("탈퇴") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteAccount() {
        val user = auth.currentUser
        val uid = user?.uid

        if (user != null && uid != null) {
            val dbRef = FirebaseDatabase.getInstance().reference
            dbRef.child("refri").child(uid).removeValue()
            dbRef.child("shopping").child(uid).removeValue()

            user.delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "회원탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        moveToLogin()
                    } else {
                        Toast.makeText(context, "탈퇴 실패: ${task.exception?.message}\n다시 로그인 후 시도해주세요.", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun moveToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}