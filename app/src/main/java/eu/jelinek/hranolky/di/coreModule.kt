package eu.jelinek.hranolky.di

import com.google.firebase.firestore.FirebaseFirestore
import org.koin.dsl.module

val coreModule get() = module {

    single {
        FirebaseFirestore.getInstance()
    }
/*
    single {
        FirebaseCrashlytics.getInstance()
    }

    single {
        FirebaseAnalytics.getInstance(get())
    }

    single {
        FirebaseAuth.getInstance()
    }

    single {
        FirebaseRemoteConfig.getInstance()
    }*/
}