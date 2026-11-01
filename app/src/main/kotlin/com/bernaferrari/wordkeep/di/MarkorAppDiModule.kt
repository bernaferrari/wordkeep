package com.bernaferrari.wordkeep.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [MarkorSharedDiModule::class])
@ComponentScan("com.bernaferrari.wordkeep.appfunctions", "com.bernaferrari.wordkeep.data.local")
class MarkorAppDiModule