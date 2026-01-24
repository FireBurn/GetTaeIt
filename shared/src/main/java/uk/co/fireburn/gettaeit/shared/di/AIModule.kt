package uk.co.fireburn.gettaeit.shared.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uk.co.fireburn.gettaeit.shared.domain.ai.GeminiNanoStrategy
import uk.co.fireburn.gettaeit.shared.domain.ai.HybridTaskService
import uk.co.fireburn.gettaeit.shared.domain.ai.TemplateStrategy
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AIModule {

    @Provides
    @Singleton
    fun provideHybridTaskService(
        template: TemplateStrategy,
        nano: GeminiNanoStrategy
    ): HybridTaskService {
        return HybridTaskService(template, nano)
    }
}
