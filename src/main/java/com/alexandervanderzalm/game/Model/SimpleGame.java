package com.alexandervanderzalm.game.Model;

import com.alexandervanderzalm.game.Model.Logger.LogCollection;
import com.alexandervanderzalm.game.Model.Logger.PitLog;
import com.alexandervanderzalm.game.Model.Logger.TextLog;
import com.alexandervanderzalm.game.Model.Pits.IKalahaPit;
import com.alexandervanderzalm.game.Model.Pits.PitCollection;
import com.alexandervanderzalm.game.Model.Pits.PitUtil;
import com.alexandervanderzalm.game.Model.Turn.TurnData;

import java.util.stream.Collectors;

public class SimpleGame implements IGame{

    //private List<IKalahaPit> pits;
    private PitCollection<IKalahaPit> pits;
    private GameState nextTurnState;
    private int currentPlayer = 0;
    private int currentTurn = 0;
    private LogCollection logger = new LogCollection();

    public SimpleGame() {
        //this.pits = new ArrayList<>();
    }

    @Override
    public TurnData SetupNewGame() {
        //int fieldsPerPlayer = 6;
        //int startStones = 6;
        pits = new PitCollection<>( PitUtil.CreatePits(14,6));
        currentTurn = 0;
        nextTurnState = GameState.TurnP1;

        // Log all the changes
        pits.pList.stream()
                .filter((p) -> !p.IsKalaha())
                .forEach((p) -> Log(p, 6));

        Log("Initialized a new kalaha game.");
        Log(String.format("Turn %d - %s new Turn",currentTurn + 1, LogPlayer(currentPlayer)));
        return GameToTurnData();
    }

    @Override
    public TurnData DoTurn(Integer SelectedIndex) {

        // Prepare gameState next round & current player index
        currentTurn++;
        currentPlayer = nextTurnState == GameState.TurnP1 ? 0 : 1;
        FlipGameState();

        // Grab all from the currently selected pit
        IKalahaPit current = pits.Get(SelectedIndex);
        Integer hand = current.GrabAll();

        // Log pickup
        Log(String.format("Turn %d - %s grabbed %d stones at %d.",currentTurn, LogPlayer(currentPlayer),hand,SelectedIndex));
        Log(current, -hand);

        // Drop one in the right pit except for the opposite players pit
        while(hand > 0) {
            //for (int i = 0; i < hand; i++) {
            current = pits.Right(current);

            // Skip when landed upon oponents kalaha
            if (current.IsKalaha() && current.GetPlayer() != currentPlayer) {
                Log(String.format("Turn %d - Skipped dropping a stone at opponents Kalaha.",currentTurn));
                continue;
            }
            if (current.GetPlayer() == currentPlayer && hand == 1) {
                // Extra turn
                if (current.IsKalaha()) {
                    //Extra turn on last stone in hand drop
                    Log(String.format("Turn %d - %s gains an Extra Turn for dropping the last stone in his own Kalaha.",currentTurn, LogPlayer(currentPlayer)));
                    FlipGameState();
                } // Capture opposite?
                else if (current.Amount() == 0) {


                    // add both opposite & the last one into own kalaha
                    IKalahaPit opposite = pits.Opposite(current);
                    int stonesCaptured = opposite.GrabAll(); // Grab first
                    IKalahaPit kalaha = pits.KalahaOfPlayer(currentPlayer);
                    kalaha.Add(stonesCaptured + 1); // Add both the hand and the captured stones to the kalaha
                    hand--;

                    // Log the same events in order
                    Log(String.format("Turn %d - %s captured %d stones from pit %d and scored %d.",currentTurn, LogPlayer(currentPlayer), stonesCaptured, pits.IndexOf(opposite), stonesCaptured+1));
                    Log(current, -1);
                    if (stonesCaptured > 0) Log(opposite, -stonesCaptured);
                    Log(kalaha, 1);
                    if (stonesCaptured > 0) Log(kalaha, stonesCaptured);

                    continue;
                }
            }

            //Log(String.format("%s Dropped one stone at pit %d",LogPlayer(currentPlayer), pits.IndexOf(current)));
            // Drop stone & log
            current.Add(1);
            hand--;
            Log(current, 1);
        }

        // Check for end of game
        // --One of the sides is empty
        if(pits.pList.stream().filter((p) -> !p.IsKalaha() && p.Amount() == 0 && p.GetPlayer() == 0).count() == 6 || pits.pList.stream().filter((p) ->!p.IsKalaha() && p.Amount() == 0 && p.GetPlayer() == 1).count() == 6) {
            // Add all pits to their respective owners
            pits.pList.stream().filter((p) -> !p.IsKalaha()).forEach((p) -> pits.KalahaOfPlayer(p.GetPlayer()).Add(p.GrabAll()));
            SetWinner();
        }

        // --Unwinnable condition detected
        int pitsInField = pits.pList.stream()
                .filter((p) -> !p.IsKalaha())
                .map((p) -> p.Amount())
                .reduce(0, (x,y) -> x + y);

        int p1 = pits.KalahaOfPlayer1().Amount();
        int p2 = pits.KalahaOfPlayer2().Amount();
        if(p1 + pitsInField < p2 || p2 + pitsInField < p1) {
            Log(String.format("Turn %d - Unwinnable condition detected. %s: %d Field: %d %s: %d.",currentTurn ,LogPlayer(0),p1,LogPlayer(1),pitsInField,p2));
            SetWinner();
        }

        // Log new turn
        if(nextTurnState == GameState.TurnP1 || nextTurnState == GameState.TurnP2)
            Log(String.format("Turn %d - %s  new Turn",currentTurn +1, LogPlayer(nextTurnState == GameState.TurnP1? 0 : 1)));

        return GameToTurnData();
    }

    private void Log(IKalahaPit pit, int amount){
        logger.Log(new PitLog(pit, pits.IndexOf(pit), amount, pit.Amount()));
    }

    private void Log(String textLog){
        logger.Log(new TextLog(textLog));
        System.out.println(textLog);
    }

    private String LogPlayer(int playerIndex){
        return  playerIndex == 0 ? "<span class = 'Player1Log'>Player1</span>" : "<span class = 'Player2Log'>Player2</span>";
    }

    private void SetWinner(){

        if(pits.KalahaOfPlayer1().Amount() > pits.KalahaOfPlayer2().Amount()) {
            nextTurnState = GameState.WinP1;
            currentPlayer = 0;
        }else {
            nextTurnState = GameState.WinP2;
            currentPlayer = 1;
        }
        Log(String.format("Congratulations %s!", LogPlayer(currentPlayer)));
    }

    private void FlipGameState(){
        nextTurnState = nextTurnState == GameState.TurnP1 ? GameState.TurnP2 : GameState.TurnP1;
    }

    private TurnData GameToTurnData(){
        TurnData data = new TurnData();
        // Transform pit data into clean rest data
        // data.Pits = pits.stream().map(x -> new KalahaPitData(x.GetPlayer(),x.IsKalaha(),x.Amount())).collect(Collectors.toList());
        data.Pits = pits.pList.stream().map(x -> x.Data()).collect(Collectors.toList());
        data.NextTurnState = nextTurnState;
        data.Turn = currentTurn;
        data.Player1Score = pits.KalahaOfPlayer1().Amount();//pits.Get(0).Amount();
        data.Player2Score = pits.KalahaOfPlayer2().Amount();// pits.Get(pits.Pits.size()/2).Amount();
        data.Log = logger.GetLogData();
        logger.ClearLogs();
        return data;
    }
}
