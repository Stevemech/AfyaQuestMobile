package com.afyaquest.app

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Empty host activity for instrumented Compose tests of screens that call `hiltViewModel()`.
 * A plain ComponentActivity cannot provide Hilt ViewModels. Debug builds only: it is not part of
 * the release app and cannot be started from outside the app.
 */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
