package control;

import boardifier.control.Controller;
import boardifier.control.Decider;
import boardifier.model.GameElement;
import boardifier.model.Model;
import boardifier.model.action.ActionList;
import control.algos.*;
import model.Move;
import model.Pawn;
import model.TablutBoard;
import model.TablutStageModel;

import java.util.List;

public class NegaMonteCarloDecider extends Decider {
    private int level;


    public NegaMonteCarloDecider(Model model, Controller control, int level) {
        super(model, control);
        this.level = level;
    }

    public NegaMonteCarloDecider(Model model, Controller controller) {
        this(model, controller, 5);
    }


    public int getLevel() { return level; }




    @Override
    public ActionList decide() {
        // do a cast get a variable of the real type to get access to the attributes of HoleStageModel
        TablutStageModel stage = (TablutStageModel)model.getGameStage();
        TablutController tablutControl = (TablutController) control;
        TablutBoard tablutBoard = stage.getBoard(); // get the board

        int turn = model.getIdPlayer();



        NegaMonteCarlo.resetBuffers();
        NegaMonteCarlo.configure(level, tablutBoard);
        int bestMoveInt = NegaMonteCarlo.findBestMove(tablutBoard, turn, ((TablutController) control).isBoardRepeated());

        int src = bestMoveInt & 0x7F;
        int dst = (bestMoveInt >> 7) & 0x7F;


        Pawn pawn = (Pawn) tablutBoard.getElement(src / 9, src % 9);


        if (pawn.getColor() == Pawn.PAWN_KING) {
            tablutBoard.setKingY(dst / 9);
            tablutBoard.setKingX(dst % 9);
        }
        (pawn).setBoardX(dst % 9);
        (pawn).setBoardY(dst / 9);
        tablutControl.getMoveHistoryIterator().add(new Move(tablutBoard, src % 9, src / 9, dst % 9, dst / 9));



        return tablutControl.genMoveAnimationWithCaptures(model, pawn, tablutBoard, dst / 9, dst % 9, false);
    }
}
