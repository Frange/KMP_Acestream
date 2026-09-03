package com.frange.kmpacestream.di

import com.frange.kmpacestream.data.repository.AndroidAceStreamRepository
import com.frange.kmpacestream.platform.AceStreamAndroidController
import com.frange.kmpacestream.shared.domain.repository.AceStreamRepository
import org.koin.dsl.module

val androidModule = module {
    single { AceStreamAndroidController(get()) }
    single<AceStreamRepository> { AndroidAceStreamRepository(get()) }
}