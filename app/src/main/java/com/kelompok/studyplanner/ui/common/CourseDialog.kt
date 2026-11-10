package com.kelompok.studyplanner.ui.common

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kelompok.studyplanner.data.CourseRepository
import com.kelompok.studyplanner.databinding.DialogCourseBinding

/** Dialog kecil untuk menambah mata kuliah baru. */
object CourseDialog {

    fun show(context: Context, onSaved: (String) -> Unit = {}) {
        val b = DialogCourseBinding.inflate(LayoutInflater.from(context))

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Mata kuliah baru")
            .setView(b.root)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan", null) // diatur ulang di bawah agar dialog tidak menutup saat input salah
            .create()
        dialog.show()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val name = b.etCourseName.text.toString().trim()
            if (name.isEmpty()) {
                b.tilCourseName.error = "Nama wajib diisi"
                return@setOnClickListener
            }
            val lecturer = b.etLecturer.text.toString().trim()
            CourseRepository.add(name, lecturer, onSaved)
            dialog.dismiss()
        }
    }
}
