<template>
  <div class="game-container">
    <h1 class="game-title">五子棋游戏</h1>
    
    <Board 
      :board="board" 
      :board-size="boardSize" 
      :game-over="gameOver"
      @place-piece="handlePlacePiece"
    />
    
    <GameControls 
      :current-player="currentPlayer"
      :game-over="gameOver"
      :winner="winner"
      :game-mode="gameMode"
      :difficulty="difficulty"
      @restart="handleRestart"
      @undo="handleUndo"
      @change-mode="handleChangeMode"
      @change-difficulty="handleChangeDifficulty"
    />
  </div>
</template>

<script>
import { useGame } from './composables/useGame';
import Board from './components/Board.vue';
import GameControls from './components/GameControls.vue';

export default {
  name: 'App',
  components: {
    Board,
    GameControls
  },
  setup() {
    const { 
      board, 
      currentPlayer, 
      gameOver, 
      winner, 
      gameMode, 
      difficulty, 
      boardSize,
      placePiece, 
      undoMove, 
      restartGame, 
      setGameMode, 
      setDifficulty 
    } = useGame();
    
    const handlePlacePiece = (row, col) => {
      placePiece(row, col);
    };
    
    const handleRestart = () => {
      restartGame();
    };
    
    const handleUndo = () => {
      undoMove();
    };
    
    const handleChangeMode = (mode) => {
      setGameMode(mode);
    };
    
    const handleChangeDifficulty = (level) => {
      setDifficulty(level);
    };
    
    return {
      board,
      currentPlayer,
      gameOver,
      winner,
      gameMode,
      difficulty,
      boardSize,
      handlePlacePiece,
      handleRestart,
      handleUndo,
      handleChangeMode,
      handleChangeDifficulty
    };
  }
};
</script>

<style>
@import './assets/styles/game.css';

#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  margin-top: 20px;
}

.game-container {
  max-width: 500px;
  margin: 0 auto;
  padding: 20px;
}

.game-title {
  text-align: center;
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #1a237e;
}
</style>
