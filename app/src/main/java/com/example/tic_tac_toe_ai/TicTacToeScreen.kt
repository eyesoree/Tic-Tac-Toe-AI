package com.example.tic_tac_toe_ai

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.tic_tac_toe_ai.ui.theme.background
import com.example.tic_tac_toe_ai.ui.theme.primary
import dashedBorder

@Composable
fun TicTacToeScreen(modifier: Modifier = Modifier, gameViewModel: GameViewModel) {
    val gameState by gameViewModel.gameState.collectAsState()
    val boardState by gameViewModel.boardState.collectAsState()
    val currentPlayer by gameViewModel.currentPlayer.collectAsState()
    val difficulty by gameViewModel.difficulty.collectAsState()
    val isInteractive = (gameState is GameState.Idle) || (gameState is GameState.Loading)

    val isBoardEmpty = boardState.all { it == Player.NONE }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .padding(32.dp)
    ) {
        DifficultySelector(
            selectedDifficulty = difficulty,
            onDifficultySelected = { gameViewModel.setDifficulty(it) },
            enabled = isBoardEmpty
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurrentPlayerCard(
                currentPlayer = currentPlayer,
                label = "Player",
                player = Player.USER,
                component = R.drawable.x_comp,
                enabled = isInteractive
            )
            Spacer(modifier = Modifier.width(24.dp))
            CurrentPlayerCard(
                currentPlayer = currentPlayer,
                label = "AI",
                player = Player.AI,
                component = R.drawable.o_comp,
                enabled = isInteractive
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Board(
            boardState = boardState,
            onCellClicked = { index -> gameViewModel.userMove(index) },
            enabled = isInteractive
        )
    }

    GameStateOverlay(gameState = gameState, onReset = { gameViewModel.resetGame() })
}

@Composable
fun DifficultySelector(
    selectedDifficulty: Difficulty,
    onDifficultySelected: (Difficulty) -> Unit,
    enabled: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 16.dp)
            .background(Color.LightGray, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Difficulty.entries.forEach { difficulty ->
            val isSelected = selectedDifficulty == difficulty
            Text(
                text = difficulty.name,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable(enabled) { onDifficultySelected(difficulty) }
                    .background(
                        color = if (isSelected) Color.DarkGray else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                color = if (isSelected) Color.White else Color.Black,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun GameStateOverlay(
    gameState: GameState,
    onReset: () -> Unit
) {
    val context = LocalContext.current

    when (gameState) {
        is GameState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color(0x80000000)), // Semi-transparent background
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "AI is thinking...",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
            }
        }

        is GameState.Error -> {
            val errorMessage = gameState.message
            Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
            onReset()
        }

        is GameState.Success -> {
            val winner = gameState.winner
            WinnerDialog(winner = winner) {
                onReset()
            }
        }

        is GameState.Idle -> {}
    }
}

@Composable
fun WinnerDialog(winner: Player, onDismiss: () -> Unit) {
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                onDismiss()
            },
            title = {
                Text(text = "Game Over")
            },
            text = {
                Text(
                    text = when (winner) {
                        Player.USER -> "You Won!"
                        Player.AI -> "AI Won!"
                        Player.NONE -> "It's a Draw!"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        onDismiss()
                    },
                ) {
                    Text(text = "Play Again")
                }
            }
        )
    }
}

@Composable
fun CurrentPlayerCard(
    modifier: Modifier = Modifier,
    currentPlayer: Player,
    label: String = "Player",
    player: Player = Player.USER,
    component: Int = R.drawable.x_comp,
    enabled: Boolean = true
) {
    val isActive = currentPlayer == player

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .border(
                width = 1.5.dp,
                color = when {
                    isActive -> Color.White
                    enabled -> Color.Gray.copy(alpha = 0.5f)
                    else -> Color.Gray.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(24.dp)
            )
            .padding(24.dp)
    ) {
        Image(
            painter = painterResource(id = component),
            contentDescription = null,
            modifier = Modifier
                .size(65.dp)
                .alpha(
                    when {
                        isActive -> 1f
                        enabled -> 0.7f
                        else -> 0.5f
                    }
                )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = label.uppercase(),
            fontWeight = FontWeight.Bold,
            color = when {
                isActive -> Color.White
                enabled -> Color.Gray.copy(alpha = 0.7f)
                else -> Color.Gray.copy(alpha = 0.5f)
            }
        )
    }
}

@Composable
fun Board(
    boardState: List<Player>,
    onCellClicked: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(280.dp)
            .background(
                color = if (enabled) primary else primary.copy(alpha = 0.5f),
                RoundedCornerShape(18.dp)
            )
            .dashedBorder(
                color = if (enabled) Color.White else Color.Gray,
                shape = RoundedCornerShape(18.dp),
                dashLength = 36.dp,
                gapLength = 36.dp
            )
    ) {
        BoardCell(
            boardState = boardState,
            onCellClicked = onCellClicked,
            enabled = enabled,
            cellSize = (280 / 3).dp
        )
        BoardGrid(enabled = enabled)
    }
}

@Composable
fun BoardCell(
    onCellClicked: (Int) -> Unit,
    boardState: List<Player>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cellSize: Dp
) {
    for (x in (0..2)) {
        for (y in 0..2) {
            val index = x * 3 + y
            val player = boardState[index]
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
                    .size(cellSize)
                    .offset(x = cellSize * x, y = cellSize * y)
                    .clickable(
                        enabled = enabled && player == Player.NONE,
                        onClick = { onCellClicked(index) }
                    )
            ) {
                player.getDrawable()?.let { drawable ->
                    Image(
                        painter = painterResource(id = drawable),
                        contentDescription = null,
                        modifier = Modifier.size(55.dp)
                    )
                }
            }
        }
    }
}

private fun Player.getDrawable(): Int? = when (this) {
    Player.USER -> R.drawable.x_comp
    Player.AI -> R.drawable.o_comp
    Player.NONE -> null
}

@Composable
fun BoardGrid(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gridColor: Color = Color.White,
) {
    val gridLineColor = if (enabled) gridColor else gridColor.copy(alpha = 0.5f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val gridSize = size.width
        val cellSize = gridSize / 3

        val lineGap = 18.dp.toPx()
        val pathEffect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(16.dp.toPx(), 6.dp.toPx()),
            phase = 0f
        )

        // helper function
        fun drawGridLine(start: Offset, end: Offset) {
            drawLine(
                color = gridLineColor,
                start = start,
                end = end,
                strokeWidth = 1.dp.toPx(),
                pathEffect = pathEffect
            )
        }

        // draw horizontal and vertical grid lines
        for (i in 1..2) {
            val offset = i * cellSize
            val gridLine = gridSize - lineGap
            drawGridLine(Offset(offset, lineGap), Offset(offset, gridLine))
            drawGridLine(Offset(lineGap, offset), Offset(gridLine, offset))
        }
    }
}