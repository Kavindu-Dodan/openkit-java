/**
 * Copyright 2018 Dynatrace LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dynatrace.openkit.core;

import com.dynatrace.openkit.api.Action;
import com.dynatrace.openkit.api.Logger;
import com.dynatrace.openkit.protocol.Beacon;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests the root action having some knowledge of the internals of the underlying actions.
 */
public class RootActionImplTest {

    private Logger logger;

    @Before
    public void setUp() {
        logger = mock(Logger.class);
        when(logger.isInfoEnabled()).thenReturn(true);
        when(logger.isDebugEnabled()).thenReturn(true);
    }

    @Test
    public void enterActionWithNullNameGivesNullAction() {
        // create test environment
        final Beacon beacon = mock(Beacon.class);
        final SynchronizedQueue<Action> actions = new SynchronizedQueue<Action>();

        // create root action with a null child action (must be valid)
        final String rootActionStr = "rootAction";
        final RootActionImpl rootAction = new RootActionImpl(logger, beacon, rootActionStr, actions);
        final Action childAction = rootAction.enterAction(null);

        // child leaves immediately
        assertThat(childAction, is(instanceOf(NullAction.class)));
        verify(logger, times(1)).warning(
                "RootActionImpl [sn=0, id=0, name=rootAction] enterAction: actionName must not be null or empty");
    }

    @Test
    public void enterActionWithEmptyNameGivesNullAction() {
        // create test environment
        final Beacon beacon = mock(Beacon.class);
        final SynchronizedQueue<Action> actions = new SynchronizedQueue<Action>();

        // create root action with a null child action (must be valid)
        final String rootActionStr = "rootAction";
        final RootActionImpl rootAction = new RootActionImpl(logger, beacon, rootActionStr, actions);
        final Action childAction = rootAction.enterAction("");

        // child leaves immediately
        assertThat(childAction, is(instanceOf(NullAction.class)));
        verify(logger, times(1)).warning(
                "RootActionImpl [sn=0, id=0, name=rootAction] enterAction: actionName must not be null or empty");
    }

    @Test
    public void enterAndLeaveActions() {
        // create test environment
        final Beacon beacon = mock(Beacon.class);
        final SynchronizedQueue<Action> actions = new SynchronizedQueue<Action>();

        // create root action with child action
        final String rootActionStr = "rootAction";
        final String childActionStr = "childAction";
        final RootActionImpl rootAction = new RootActionImpl(logger, beacon, rootActionStr, actions);
        final Action childAction = rootAction.enterAction(childActionStr);

        // verify
        assertThat(rootAction, is(instanceOf(ActionImpl.class)));
        assertThat(childAction, is(instanceOf(ActionImpl.class)));
        assertThat(rootAction.getName(), is(rootActionStr));
        assertThat(((ActionImpl) childAction).getName(), is(childActionStr));

        // child leaves
        Action retAction = childAction.leaveAction();
        assertThat(retAction, is(sameInstance((Action) rootAction)));

        // parent leaves
        retAction = rootAction.leaveAction();
        assertThat(retAction, is(nullValue()));

        // verify that open child actions are now empty
        assertThat(actions.toArrayList().isEmpty(), is(true));
    }

    @Test
    public void enterAndLeaveActionsWithMultipleChildren() {
        // create test environment
        final Beacon beacon = mock(Beacon.class);
        final SynchronizedQueue<Action> actions = new SynchronizedQueue<Action>();

        // create root action with 2 children
        final String rootActionStr = "rootAction";
        final String childOneActionStr = "childOneAction";
        final String childTwoActionStr = "childTwoAction";
        final RootActionImpl rootAction = new RootActionImpl(logger, beacon, rootActionStr, actions);
        final Action childAction1 = rootAction.enterAction(childOneActionStr);
        final Action childAction2 = rootAction.enterAction(childTwoActionStr);

        // verify (using internally known methods)
        assertThat(rootAction, is(instanceOf(ActionImpl.class)));
        assertThat(childAction1, is(instanceOf(ActionImpl.class)));
        assertThat(childAction2, is(instanceOf(ActionImpl.class)));
        assertThat(rootAction.getName(), is(rootActionStr));
        assertThat(((ActionImpl) childAction1).getName(), is(childOneActionStr));
        assertThat(((ActionImpl) childAction2).getName(), is(childTwoActionStr));

        // parent leaves, thus the children are also left
        final Action retAction = rootAction.leaveAction();
        assertThat(retAction, is(nullValue()));
        assertThat(actions.toArrayList().isEmpty(), is(true));
    }

    @Test
    public void enterActionGivesNullActionIfAlreadyLeft() {

        // given
        RootActionImpl target = new RootActionImpl(logger, mock(Beacon.class),
            "parent action", new SynchronizedQueue<Action>());
        target.leaveAction();

        // when
        Action obtained = target.enterAction("child action");

        // then
        assertThat(obtained, is(notNullValue()));
        assertThat(obtained, is(instanceOf(NullAction.class)));
    }
}
