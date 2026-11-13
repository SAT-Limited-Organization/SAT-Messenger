package com.example.ui.screens.auth

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.AuthProvider
import com.example.data.repository.AuthUiState
import com.example.ui.components.ErrorCard
import com.example.ui.theme.GoogleBrandBlue
import com.example.ui.theme.GoogleBrandGreen
import com.example.ui.theme.GoogleBrandRed
import com.example.ui.theme.GoogleBrandYellow
import com.example.ui.theme.MicrosoftBrandBlue
import com.example.ui.theme.MicrosoftBrandGreen
import com.example.ui.theme.MicrosoftBrandRed
import com.example.ui.theme.MicrosoftBrandYellow
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatNavy
import com.example.ui.theme.SatTeal

@Composable
fun SignInScreen(
    authState: AuthUiState,
    activeProvider: AuthProvider? = null,
    onGoogleSignInClick: (Activity) -> Unit,
    onMicrosoftSignInClick: (Activity) -> Unit,
    onDismissError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLoading = authState is AuthUiState.Loading
    val isGoogleLoading = isLoading && (activeProvider == AuthProvider.GOOGLE || activeProvider == null)
    val isMicrosoftLoading = isLoading && activeProvider == AuthProvider.MICROSOFT

    val bgPrimary = MaterialTheme.colorScheme.background
    val accentTop = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val accentTeal = SatTeal.copy(alpha = 0.07f)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = bgPrimary
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(accentTop, bgPrimary, accentTeal)
                        )
                    )
                }
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // SAT Brand Emblem
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SatNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.sat_messenger_logo),
                        contentDescription = "SAT Messenger Logo",
                        modifier = Modifier.size(60.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "SAT LIMITED NETWORK",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    color = SatTeal
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Secure instant messaging, real-time collaboration, and company directory for SAT Limited personnel.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Security status pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = SatElectricBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Encrypted Cloud Firestore Storage",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Sign-In Methods Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Sign in to SAT MESSENGER",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Choose Google or your Microsoft account",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Current Method: Google Sign-In via Credential Manager
                        OutlinedButton(
                            onClick = {
                                (context as? Activity)?.let { onGoogleSignInClick(it) }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = 1.2.dp,
                                color = MaterialTheme.colorScheme.outline
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isGoogleLoading) {
                                CircularProgressIndicator(
                                    color = SatElectricBlue,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    GoogleLogoMark(modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.sign_in_google),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                            Text(
                                text = stringResource(R.string.sign_in_or_divider),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // New Method: Microsoft Sign-In via Firebase OAuthProvider
                        Button(
                            onClick = {
                                (context as? Activity)?.let { onMicrosoftSignInClick(it) }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SatNavy,
                                contentColor = Color.White,
                                disabledContainerColor = SatNavy.copy(alpha = 0.5f),
                                disabledContentColor = Color.White.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("microsoft_sign_in_button")
                        ) {
                            if (isMicrosoftLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    MicrosoftLogoMark(modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.sign_in_microsoft),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                if (authState is AuthUiState.Error) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ErrorCard(
                        message = authState.message,
                        onDismiss = onDismissError
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Compliant with SAT Limited Data Governance and Access Control Policies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun GoogleLogoMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.22f
        val inset = strokeWidth / 2f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(inset, inset)

        // Red top arc
        drawArc(
            color = GoogleBrandRed,
            startAngle = -145f,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Yellow left arc
        drawArc(
            color = GoogleBrandYellow,
            startAngle = 140f,
            sweepAngle = 75f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Green bottom arc
        drawArc(
            color = GoogleBrandGreen,
            startAngle = 45f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Blue right arc + crossbar
        drawArc(
            color = GoogleBrandBlue,
            startAngle = -10f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        drawLine(
            color = GoogleBrandBlue,
            start = Offset(size.width * 0.50f, size.height * 0.50f),
            end = Offset(size.width - inset * 0.4f, size.height * 0.50f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt
        )
    }
}

@Composable
private fun MicrosoftLogoMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val gap = size.minDimension * 0.1f
        val tileWidth = (size.width - gap) / 2f
        val tileHeight = (size.height - gap) / 2f
        val tileSize = Size(tileWidth, tileHeight)

        // Top-left: Red
        drawRect(
            color = MicrosoftBrandRed,
            topLeft = Offset.Zero,
            size = tileSize
        )
        // Top-right: Green
        drawRect(
            color = MicrosoftBrandGreen,
            topLeft = Offset(tileWidth + gap, 0f),
            size = tileSize
        )
        // Bottom-left: Blue
        drawRect(
            color = MicrosoftBrandBlue,
            topLeft = Offset(0f, tileHeight + gap),
            size = tileSize
        )
        // Bottom-right: Yellow
        drawRect(
            color = MicrosoftBrandYellow,
            topLeft = Offset(tileWidth + gap, tileHeight + gap),
            size = tileSize
        )
    }
}
