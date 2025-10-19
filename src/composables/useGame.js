import { ref } from 'vue';
import { useBoard } from './useBoard';
import { useAI } from './useAI';

// 游戏核心逻辑组合式函数
export function useGame() {
  const { createBoard, isValidMove, updateBoard } = useBoard();
  const { getAIMove } = useAI();
  
  const boardSize = 15;
  const board = ref(createBoard(boardSize));
  const currentPlayer = ref(1); // 1=黑, 2=白
  const gameOver = ref(false);
  const winner = ref(null);
  const history = ref([]);
  const gameMode = ref('human'); // 'human' or 'ai'
  const difficulty = ref(1);
  
  // 初始化棋盘
  const initBoard = () => {
    board.value = createBoard(boardSize);
    currentPlayer.value = 1;
    gameOver.value = false;
    winner.value = null;
    history.value = [];
  };
  
  // 落子
  const placePiece = (row, col) => {
    if (gameOver.value || !isValidMove(board.value, row, col)) {
      return false;
    }
    
    // 更新棋盘
    updateBoard(board.value, row, col, currentPlayer.value);
    
    // 记录历史
    history.value.push({ row, col, player: currentPlayer.value });
    
    // 检查胜负
    if (checkWin(row, col)) {
      gameOver.value = true;
      winner.value = currentPlayer.value;
      return true;
    }
    
    // 切换玩家
    switchPlayer();
    
    // AI落子
    if (gameMode.value === 'ai' && currentPlayer.value === 2 && !gameOver.value) {
      setTimeout(() => {
        const aiMove = getAIMove(board.value, currentPlayer.value, difficulty.value);
        placePiece(aiMove.row, aiMove.col);
      }, 500);
    }
    
    return true;
  };
  
  // 检查胜负
  const checkWin = (row, col) => {
    const player = board.value[row][col];
    const directions = [
      [0, 1],   // 横向
      [1, 0],   // 纵向
      [1, 1],   // 左上-右下
      [1, -1]   // 右上-左下
    ];
    
    for (const [dx, dy] of directions) {
      let count = 1;
      
      // 正方向
      for (let i = 1; i < 5; i++) {
        const newRow = row + i * dx;
        const newCol = col + i * dy;
        if (newRow >= 0 && newRow < boardSize && newCol >= 0 && newCol < boardSize && board.value[newRow][newCol] === player) {
          count++;
        } else {
          break;
        }
      }
      
      // 反方向
      for (let i = 1; i < 5; i++) {
        const newRow = row - i * dx;
        const newCol = col - i * dy;
        if (newRow >= 0 && newRow < boardSize && newCol >= 0 && newCol < boardSize && board.value[newRow][newCol] === player) {
          count++;
        } else {
          break;
        }
      }
      
      if (count >= 5) {
        return true;
      }
    }
    
    return false;
  };
  
  // 切换玩家
  const switchPlayer = () => {
    currentPlayer.value = currentPlayer.value === 1 ? 2 : 1;
  };
  
  // 悔棋
  const undoMove = () => {
    if (history.value.length === 0 || gameOver.value) {
      return false;
    }
    
    // 撤销上一步
    const lastMove = history.value.pop();
    board.value[lastMove.row][lastMove.col] = 0;
    
    // 切换回上一玩家
    switchPlayer();
    
    // 若为AI模式，再撤销一步AI的棋
    if (gameMode.value === 'ai' && history.value.length > 0) {
      const aiMove = history.value.pop();
      board.value[aiMove.row][aiMove.col] = 0;
      switchPlayer();
    }
    
    return true;
  };
  
  // 重新开始
  const restartGame = () => {
    initBoard();
  };
  
  // 切换游戏模式
  const setGameMode = (mode) => {
    gameMode.value = mode;
    initBoard();
  };
  
  // 设置AI难度
  const setDifficulty = (level) => {
    difficulty.value = level;
  };
  
  return {
    board,
    currentPlayer,
    gameOver,
    winner,
    gameMode,
    difficulty,
    boardSize,
    initBoard,
    placePiece,
    undoMove,
    restartGame,
    setGameMode,
    setDifficulty
  };
}