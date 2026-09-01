package com.frange.kmpacestream.shared.di

import com.frange.kmpacestream.shared.data.remote.AceStreamApiClient
import com.frange.kmpacestream.shared.data.remote.HttpClientFactory
import com.frange.kmpacestream.shared.data.repository.AceStreamRepositoryImpl
import com.frange.kmpacestream.shared.domain.repository.AceStreamRepository
import org.koin.dsl.module

val sharedModule = module {
    single { HttpClientFactory.create() }
    single { AceStreamApiClient(get()) }
    single<AceStreamRepository> { AceStreamRepositoryImpl(get()) }
}