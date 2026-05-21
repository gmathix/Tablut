package model;

import boardifier.model.GameStageModel;
import boardifier.model.TextElement;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class TablutStageFactoryUnitTest {
    @Test
    public void TestSetup(){
        TextElement text = Mockito.mock(TextElement.class);
        TablutBoard board = Mockito.mock(TablutBoard.class);
        GameStageModel gameStageModel = Mockito.mock(GameStageModel.class)
        TablutStageFactory tablutStageFactory = new TablutStageFactory(gameStageModel);

    }
}