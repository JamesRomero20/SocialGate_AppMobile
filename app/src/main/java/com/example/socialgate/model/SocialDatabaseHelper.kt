package com.example.socialgate.model

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SocialDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 2
        private const val DATABASE_NAME = "SocialMediaControl.db"
        const val TABLE_USUARIO = "usuario"
        const val TABLE_ACTIVIDAD = "actividad"
        const val TABLE_HORARIO_BLOQUEO = "horario_bloqueo"
        const val KEY_ID_HORARIO = "id_horario_pk"
        const val TABLE_CONTROL_TIEMPO = "control_tiempo"
        const val TABLE_REPORTE = "reporte"
        const val TABLE_NOTIFICACION = "notificacion"
        const val KEY_DIA_SEMANA = "dia_semana"
        const val KEY_HORA_INICIO = "hora_inicio"
        const val KEY_HORA_FIN = "hora_fin"
        const val KEY_HORARIO_ACTIVO = "horario_activo"
        const val KEY_ID_USUARIO_FK = "id_usuario"
        const val KEY_ID_USUARIO = "id_usuario_pk"
        const val KEY_NOMBRE = "nombre"
        const val KEY_USUARIO = "usuario"
        const val KEY_CLAVE = "clave"
        const val KEY_TIPO_USUARIO = "tipo_usuario"
        const val KEY_EMAIL = "email"
        const val KEY_FECHA_REGISTRO = "fecha_registro"
        const val KEY_ID_ACTIVIDAD = "id_actividad_pk"
        const val KEY_RED_SOCIAL_ACTIVIDAD = "red_social"
        const val KEY_FECHA_HORA = "fecha_hora"
        const val KEY_DURACION = "duracion"
        const val KEY_DESCRIPCION = "descripcion"
        const val KEY_ID_CONTROL = "id_control_pk"
        const val KEY_RED_SOCIAL_CONTROL = "red_social"
        const val KEY_TIEMPO_LIMITE = "tiempo_limite"
        const val KEY_TIEMPO_USADO = "tiempo_usado"
        const val KEY_ESTADO_ALERTA = "estado_alerta"
        const val KEY_ID_REPORTE = "id_reporte_pk"
        const val KEY_FECHA_GENERACION = "fecha_generacion"
        const val KEY_TIPO_REPORTE = "tipo_reporte"
        const val KEY_CONTENIDO = "contenido"
        const val KEY_ID_NOTIFICACION = "id_notificacion_pk"
        const val KEY_TIPO_NOTIFICACION = "tipo"
        const val KEY_MENSAJE = "mensaje"
        const val KEY_FECHA_ENVIO = "fecha_envio"
        const val KEY_ESTADO_NOTIFICACION = "estado"
    }

    override fun onCreate(db: SQLiteDatabase?) {

        val createUsuarioTable = ("CREATE TABLE " + TABLE_USUARIO + "("
                + KEY_ID_USUARIO + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_NOMBRE + " TEXT,"
                + KEY_USUARIO + " TEXT UNIQUE,"
                + KEY_CLAVE + " TEXT,"
                + KEY_TIPO_USUARIO + " TEXT,"
                + KEY_EMAIL + " TEXT UNIQUE,"
                + KEY_FECHA_REGISTRO + " TEXT" + ")")
        db?.execSQL(createUsuarioTable)

        val createActividadTable = ("CREATE TABLE " + TABLE_ACTIVIDAD + "("
                + KEY_ID_ACTIVIDAD + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_ID_USUARIO_FK + " INTEGER,"
                + KEY_RED_SOCIAL_ACTIVIDAD + " TEXT,"
                + KEY_FECHA_HORA + " TEXT,"
                + KEY_DURACION + " INTEGER,"
                + KEY_DESCRIPCION + " TEXT,"
                + "FOREIGN KEY(" + KEY_ID_USUARIO_FK + ") REFERENCES " + TABLE_USUARIO + "(" + KEY_ID_USUARIO + ")" + ")")
        db?.execSQL(createActividadTable)

        val createControlTiempoTable = ("CREATE TABLE " + TABLE_CONTROL_TIEMPO + "("
                + KEY_ID_CONTROL + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_ID_USUARIO_FK + " INTEGER,"
                + KEY_RED_SOCIAL_CONTROL + " TEXT,"
                + KEY_TIEMPO_LIMITE + " INTEGER,"
                + KEY_TIEMPO_USADO + " INTEGER,"
                + KEY_ESTADO_ALERTA + " TEXT,"
                + "FOREIGN KEY(" + KEY_ID_USUARIO_FK + ") REFERENCES " + TABLE_USUARIO + "(" + KEY_ID_USUARIO + ")" + ")")
        db?.execSQL(createControlTiempoTable)

        val createReporteTable = ("CREATE TABLE " + TABLE_REPORTE + "("
                + KEY_ID_REPORTE + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_ID_USUARIO_FK + " INTEGER,"
                + KEY_FECHA_GENERACION + " TEXT,"
                + KEY_TIPO_REPORTE + " TEXT,"
                + KEY_CONTENIDO + " TEXT,"
                + "FOREIGN KEY(" + KEY_ID_USUARIO_FK + ") REFERENCES " + TABLE_USUARIO + "(" + KEY_ID_USUARIO + ")" + ")")
        db?.execSQL(createReporteTable)

        val createNotificacionTable = ("CREATE TABLE " + TABLE_NOTIFICACION + "("
                + KEY_ID_NOTIFICACION + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_ID_USUARIO_FK + " INTEGER,"
                + KEY_TIPO_NOTIFICACION + " TEXT,"
                + KEY_MENSAJE + " TEXT,"
                + KEY_FECHA_ENVIO + " TEXT,"
                + KEY_ESTADO_NOTIFICACION + " TEXT,"
                + "FOREIGN KEY(" + KEY_ID_USUARIO_FK + ") REFERENCES " + TABLE_USUARIO + "(" + KEY_ID_USUARIO + ")" + ")")
        db?.execSQL(createNotificacionTable)

        val createHorarioBloqueoTable = ("CREATE TABLE " + TABLE_HORARIO_BLOQUEO + "("
                + KEY_ID_HORARIO + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_ID_USUARIO_FK + " INTEGER,"
                + KEY_DIA_SEMANA + " TEXT,"
                + KEY_HORA_INICIO + " TEXT,"
                + KEY_HORA_FIN + " TEXT,"
                + KEY_HORARIO_ACTIVO + " INTEGER DEFAULT 1,"
                + "FOREIGN KEY(" + KEY_ID_USUARIO_FK + ") REFERENCES " + TABLE_USUARIO + "(" + KEY_ID_USUARIO + ")" + ")")
        db?.execSQL(createHorarioBloqueoTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

        db?.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICACION)
        db?.execSQL("DROP TABLE IF EXISTS " + TABLE_REPORTE)
        db?.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTROL_TIEMPO)
        db?.execSQL("DROP TABLE IF EXISTS " + TABLE_ACTIVIDAD)
        db?.execSQL("DROP TABLE IF EXISTS " + TABLE_HORARIO_BLOQUEO)
        db?.execSQL("DROP TABLE IF EXISTS " + TABLE_USUARIO)

        onCreate(db)
    }
}