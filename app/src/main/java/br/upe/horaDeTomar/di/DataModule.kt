package br.upe.horaDeTomar.di

import br.upe.horaDeTomar.data.remote.FhirDataSource
import br.upe.horaDeTomar.data.remote.FhirDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindFhirDataSource(
        fhirDataSourceImpl: FhirDataSourceImpl
    ): FhirDataSource
}