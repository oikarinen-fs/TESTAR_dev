package nl.ou.testar.ReinforcementLearning.RewardFunctions;

import nl.ou.testar.StateModel.AbstractAction;
import nl.ou.testar.StateModel.AbstractState;
import nl.ou.testar.StateModel.ConcreteState;
import org.fruit.alayer.Action;
import org.fruit.alayer.State;

import java.util.Set;


/**
 * Interface for reward function implementation
 */
public interface RewardFunction {

    /**
     * Get the reward for a given action
     *
     *
     * @param state
     * @param currentConcreteState The {@link ConcreteState} the SUT is in
     * @param currentAbstractState The {@link AbstractState} the SUT is in
     * @param executedAction The {@link AbstractAction} that was executed
     * @param actions
     * @return The calculated reward
     */
    public float getReward(final State state, final ConcreteState currentConcreteState, final AbstractState currentAbstractState, final AbstractAction executedAction, Set<Action> actions);

    /**
     * Resets the reward function
     */
    public void reset();

}
