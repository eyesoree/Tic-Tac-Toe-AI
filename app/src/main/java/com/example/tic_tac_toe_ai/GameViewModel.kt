package com.example.tic_tac_toe_ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface GameState {
    data object Loading : GameState
    data object Idle : GameState
    data class Error(val message: String) : GameState
    data class Success(val winner: Player = Player.NONE) : GameState
}

enum class Player { USER, AI, NONE }
enum class Difficulty { EASY, MEDIUM, HARD }

class GameViewModel : ViewModel() {
    companion object {
        private const val BOARD_SIZE = 9
        private val WIN_COMBINATIONS = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),  // horizontal
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),  // vertical
            listOf(0, 4, 8), listOf(2, 4, 6)                    // diagonal
        )
        private const val AI_MOVE_DELAY = 500L
    }

    private val _gameState = MutableStateFlow<GameState>(GameState.Idle)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _currentPlayer = MutableStateFlow(Player.USER)
    val currentPlayer: StateFlow<Player> = _currentPlayer.asStateFlow()

    private val _boardState = MutableStateFlow(List(BOARD_SIZE) { Player.NONE })
    val boardState: StateFlow<List<Player>> = _boardState.asStateFlow()

    private val _difficulty = MutableStateFlow(Difficulty.EASY)
    val difficulty: StateFlow<Difficulty> = _difficulty.asStateFlow()

    fun setDifficulty(difficulty: Difficulty) {
        _difficulty.value = difficulty
    }

    fun userMove(index: Int) {
        if (gameState.value !is GameState.Idle || _boardState.value[index] != Player.NONE) {
            return
        }

        updateBoard(index, Player.USER)
        checkWinner()?.let {
            _gameState.value = it
            return
        }

        _currentPlayer.value = Player.AI
        _gameState.value = GameState.Loading

        viewModelScope.launch {
            delay(AI_MOVE_DELAY)
            aiMove()
        }
    }

    private fun updateBoard(index: Int, player: Player) {
        _boardState.update { currentList ->
            currentList.toMutableList().apply {
                this[index] = player
            }
        }
    }

    private fun aiMove() {
        viewModelScope.launch {
            val move = when (_difficulty.value) {
                Difficulty.EASY -> getRandomMove()
                Difficulty.MEDIUM -> getMediumDifficultyMove()
                Difficulty.HARD -> findBestMove(boardState.value)
            }

            if (move != -1) {
                updateBoard(move, Player.AI)

                checkWinner()?.let {
                    _gameState.value = it
                    return@launch
                }

                _gameState.value = GameState.Idle
                _currentPlayer.value = Player.USER
            }
        }
    }


    private fun findBestMove(board: List<Player>): Int {
        var bestVal = Int.MIN_VALUE
        var bestMove = -1

        for (i in board.indices) {
            if (board[i] == Player.NONE) {
                val newBoard = board.toMutableList()
                newBoard[i] = Player.AI
                val moveVal = minimax(newBoard, 0, false)
                if (moveVal > bestVal) {
                    bestMove = i
                    bestVal = moveVal
                }
            }
        }
        return bestMove
    }

    private fun minimax(board: List<Player>, depth: Int, isMax: Boolean): Int {
        val score = evaluateBoard(board)

        if (score == 10) return score - depth // AI is maximizing
        if (score == -10) return score + depth // User is minimizing
        if (board.all { it != Player.NONE }) return 0 // Draw

        return if (isMax) {
            var best = Int.MIN_VALUE
            for (i in board.indices) {
                if (board[i] == Player.NONE) {
                    val newBoard = board.toMutableList()
                    newBoard[i] = Player.AI
                    best = maxOf(best, minimax(newBoard, depth + 1, false))
                }
            }
            best
        } else {
            var best = Int.MAX_VALUE
            for (i in board.indices) {
                if (board[i] == Player.NONE) {
                    val newBoard = board.toMutableList()
                    newBoard[i] = Player.USER
                    best = minOf(best, minimax(newBoard, depth + 1, true))
                }
            }
            best
        }
    }

    private fun evaluateBoard(board: List<Player>): Int {
        return when {
            checkWinForPlayer(board, Player.AI) -> 10
            checkWinForPlayer(board, Player.USER) -> -10
            else -> 0
        }
    }

    // Generates a random valid move (used for EASY difficulty)
    private fun getRandomMove(): Int {
        return _boardState.value.indices.filter { _boardState.value[it] == Player.NONE }.random()
    }

    private fun getMediumDifficultyMove(): Int {
        if ((1..100).random() <= 30)
            return findBestMove(boardState.value)
        return getRandomMove()
    }

    private fun checkWinForPlayer(board: List<Player>, player: Player): Boolean {
        return WIN_COMBINATIONS.any { combination ->
            combination.all { index -> board[index] == player }
        }
    }

    private fun checkWinner(): GameState.Success? {
        for (combination in WIN_COMBINATIONS) {
            val (a, b, c) = combination
            val player = boardState.value[a]

            if ((player != Player.NONE) &&
                (boardState.value[a] == boardState.value[b]) &&
                (boardState.value[b] == boardState.value[c])
            ) {
                return GameState.Success(winner = player)
            }
        }

        if (boardState.value.none { it == Player.NONE }) {
            return GameState.Success(winner = Player.NONE)
        }

        return null
    }

    fun resetGame() {
        _gameState.value = GameState.Idle
        _boardState.value = List(9) { Player.NONE }
        _currentPlayer.value = Player.USER
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }
}