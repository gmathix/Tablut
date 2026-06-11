package control;

import boardifier.control.Controller;
import boardifier.control.Decider;
import boardifier.model.GameElement;
import boardifier.model.Model;
import boardifier.model.action.ActionList;
import control.algos.Negamax;
import control.algos.OpeningPlayer;
import model.*;

import java.util.List;

public class NegamaxDecider extends Decider {
    private int level;


    public NegamaxDecider(Model model, Controller control, int level) {
        super(model, control);
        this.level = level;

    }

    public NegamaxDecider(Model model, Controller controller) {
        this(model, controller, 5);
    }


    public int getLevel() { return level; }



    @Override
    public ActionList decide() {

        // do a cast get a variable of the real type to get access to the attributes of HoleStageModel
        TablutStageModel stage = (TablutStageModel)model.getGameStage();
        TablutController tablutControl = (TablutController) control;
        TablutBoard tablutBoard = stage.getBoard();

        int turn = model.getIdPlayer();


        int bestMoveInt;


        String moveSeq = tablutControl.getMoveHistory().toString().trim();
        int openingMove = OpeningPlayer.makeOpeningMove(moveSeq);


        boolean greenFirstMove = moveSeq.isEmpty() && turn == 0;
        if (openingMove != -1 && !greenFirstMove) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {

            }
            bestMoveInt = openingMove;
        } else {
            Negamax.resetBuffers();
            Negamax.configure(level, tablutBoard);
            bestMoveInt = Negamax.findBestMove(turn, ((TablutController) control).isBoardRepeated());
        }


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
