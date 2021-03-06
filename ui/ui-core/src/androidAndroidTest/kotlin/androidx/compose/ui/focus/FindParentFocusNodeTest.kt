/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.focus

import androidx.compose.foundation.Box
import androidx.compose.foundation.background
import androidx.compose.ui.FocusModifier2
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.test.filters.SmallTest
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnIdle
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@OptIn(ExperimentalFocus::class)
@RunWith(JUnit4::class)
class FindParentFocusNodeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun noParentReturnsNull() {
        // Arrange.
        val focusModifier = FocusModifier2(FocusState2.Inactive)
        composeTestRule.setFocusableContent {
            Box(modifier = focusModifier)
        }

        // Act.
        val rootFocusNode = runOnIdle {
            focusModifier.focusNode.findParentFocusNode2()!!.findParentFocusNode2()
        }

        // Assert.
        runOnIdle {
            assertThat(rootFocusNode).isNull()
        }
    }

    @Test
    fun returnsImmediateParentFromModifierChain() {
        // Arrange.
        // focusNode1--focusNode2--focusNode3--focusNode4--focusNode5
        val modifier1 = FocusModifier2(FocusState2.Inactive)
        val modifier2 = FocusModifier2(FocusState2.Inactive)
        val modifier3 = FocusModifier2(FocusState2.Inactive)
        val modifier4 = FocusModifier2(FocusState2.Inactive)
        val modifier5 = FocusModifier2(FocusState2.Inactive)
        composeTestRule.setFocusableContent {
            Box(modifier1.then(modifier2).then(modifier3).then(modifier4).then(modifier5)) {}
        }

        // Act.
        val parent = runOnIdle {
            modifier3.focusNode.findParentFocusNode2()
        }

        // Assert.
        runOnIdle {
            assertThat(parent).isEqualTo(modifier2.focusNode)
        }
    }

    @Test
    fun returnsImmediateParentFromModifierChain_ignoresNonFocusModifiers() {
        // Arrange.
        // focusNode1--focusNode2--nonFocusNode--focusNode3
        val modifier1 = FocusModifier2(FocusState2.Inactive)
        val modifier2 = FocusModifier2(FocusState2.Inactive)
        val modifier3 = FocusModifier2(FocusState2.Inactive)
        composeTestRule.setFocusableContent {
            Box(
                modifier = modifier1
                    .then(modifier2)
                    .background(color = Red)
                    .then(modifier3)
            )
        }

        // Act.
        val parent = runOnIdle {
            modifier3.focusNode.findParentFocusNode2()
        }

        // Assert.
        runOnIdle {
            assertThat(parent).isEqualTo(modifier2.focusNode)
        }
    }

    @Test
    fun returnsLastFocusParentFromParentLayoutNode() {
        // Arrange.
        // parentLayoutNode--parentFocusNode1--parentFocusNode2
        //       |
        // layoutNode--focusNode
        val parentFocusModifier1 = FocusModifier2(FocusState2.Inactive)
        val parentFocusModifier2 = FocusModifier2(FocusState2.Inactive)
        val focusModifier = FocusModifier2(FocusState2.Inactive)
        composeTestRule.setFocusableContent {
            Box(modifier = parentFocusModifier1.then(parentFocusModifier2)) {
                Box(modifier = focusModifier)
            }
        }

        // Act.
        val parent = runOnIdle {
            focusModifier.focusNode.findParentFocusNode2()
        }

        // Assert.
        runOnIdle {
            assertThat(parent).isEqualTo(parentFocusModifier2.focusNode)
        }
    }

    @Test
    fun returnsImmediateParent() {
        // Arrange.
        // grandparentLayoutNode--grandparentFocusNode
        //       |
        // parentLayoutNode--parentFocusNode
        //       |
        // layoutNode--focusNode
        val grandparentFocusModifier = FocusModifier2(FocusState2.Inactive)
        val parentFocusModifier = FocusModifier2(FocusState2.Inactive)
        val focusModifier = FocusModifier2(FocusState2.Inactive)
        composeTestRule.setFocusableContent {
            Box(modifier = grandparentFocusModifier) {
                Box(modifier = parentFocusModifier) {
                    Box(modifier = focusModifier)
                }
            }
        }

        // Act.
        val parent = runOnIdle {
            focusModifier.focusNode.findParentFocusNode2()
        }

        // Assert.
        runOnIdle {
            assertThat(parent).isEqualTo(parentFocusModifier.focusNode)
        }
    }

    @Test
    fun ignoresIntermediateLayoutNodesThatDontHaveFocusNodes() {
        // Arrange.
        // grandparentLayoutNode--grandparentFocusNode
        //       |
        // parentLayoutNode
        //       |
        // layoutNode--focusNode
        val grandparentFocusModifier = FocusModifier2(FocusState2.Inactive)
        val focusModifier = FocusModifier2(FocusState2.Inactive)
        composeTestRule.setFocusableContent {
            Box(modifier = grandparentFocusModifier) {
                Box {
                    Box(modifier = focusModifier)
                }
            }
        }

        // Act.
        val parent = runOnIdle {
            focusModifier.focusNode.findParentFocusNode2()
        }

        // Assert.
        runOnIdle {
            assertThat(parent).isEqualTo(grandparentFocusModifier.focusNode)
        }
    }
}
