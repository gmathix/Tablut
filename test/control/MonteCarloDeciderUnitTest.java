package control;

import boardifier.control.ActionPlayer;
import boardifier.control.Controller;
import boardifier.model.GameElement;
import boardifier.model.Model;
import boardifier.model.action.ActionList;
import control.algos.Board;
import control.algos.NegaMonteCarlo;
import model.Move;
import model.TablutBoard;
import model.TablutStageModel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class MonteCarloDeciderUnitTest {
    @Test
    public void TestDecide(){
        TablutStageModel stage = Mockito.mock(TablutStageModel.class);
        TablutBoard tablutBoard = Mockito.mock(TablutBoard.class);
        Board board = Mockito.mock(Board.class);
        NegaMonteCarlo negaMonteCarlo = Mockito.mock(NegaMonteCarlo.class);
        Move bestMove = Mockito.mock(Move.class);


        MonteCarloDecider monteCarloDecider = new MonteCarloDecider(new Model(), new Controller() {
            @Override
            public void stageLoop() {

            }
        })
    }
}
