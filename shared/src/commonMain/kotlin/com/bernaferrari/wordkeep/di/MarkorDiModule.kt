package com.bernaferrari.wordkeep.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [PlatformDiModule::class])
@ComponentScan("com.bernaferrari.wordkeep")
class MarkorSharedDiModule