/*
   Copyright 2026 WeAreFrank!, 2018 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.wearefrank.ladybug.web.common.shownreport;

import jakarta.inject.Inject;
import org.springframework.stereotype.Component;
import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.filter.View;
import org.wearefrank.ladybug.web.common.LadybugValidator;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import lombok.extern.slf4j.Slf4j;

import lombok.Setter;

/*
 * Classes Report and Checkpoint have more fields than the fields needed by the
 * frontend. Therefore we have classes ShownReport and ShownCheckpoint. This
 * class can copy a Report with Checkpoint-s to a ShownReport with
 * ShownCheckpoint-s.
 *
 * An important difference between Report and ShownReport is that the
 * checkpoints in a Report are a flat list while they are a
 * hierarchy in ShownReport. This class makes the translation based
 * on property "level". This way we do not duplicate the logic that
 * uses enum CheckpointType to organize the checkpoints.
 *
 * In addition to building the hierarchy, this class filters checkpoints
 * based on the View. The algorithm that builds the hierarchy works as follows.
 * It iterates over all checkpoints of the report. During the iteration, it
 * maintains a stack that represents the branch of the current checkpoint.
 * This branch refers to the tree of checkpoints as it would appear in the
 * white box view. That tree is not stored. Instead each checkpoint is
 * fed to the View instance to determine whether it is shown, which means
 * it is passed to the frontend. When a checkpoint is shown it is added to
 * the first ancestor of its branch that is shown.
 */
@Component
@Slf4j
public class ShownReportBuilder {
	@Autowired
	private @Setter LadybugValidator validator;

	// A node of the current checkpoint branch.
	private static abstract class Ancestor {
		final TreeNode contents;

		Ancestor(TreeNode contents) {
			this.contents = contents;
		}

		// Returns true if the white box view would show the argument checkpoint
		// inside the wrapped checkpoint or report. The end node corresponding to a start
		// node is shown inside that start node (one level deeper!). This is respected
		// by property "level" - the level of the endpoint is one higher than the level
		// of the corresponding startpoint.
		//
		// This method expects to see all checkpoints of a report. When it returns
		// false then inner class Session should remove this Ancestor from the
		// current branch.
		abstract boolean acceptChild(ShownCheckpoint checkpoint);

		abstract boolean isShown();
	}

	private static final class ReportAncestor extends Ancestor {
		ReportAncestor(ShownReport report) {
			super(report);
		}

		@Override
		boolean acceptChild(ShownCheckpoint checkpoint) {
			return true;
		}

		@Override
		boolean isShown() {
			return true;
		}
	}

	private static final class CheckpointAncestor extends Ancestor {
		private final int level;
		private boolean shown;

		CheckpointAncestor(ShownCheckpoint checkpoint, boolean shown) {
			super(checkpoint);
			this.level = checkpoint.getLevel();
			this.shown = shown;
		}

		@Override
		boolean isShown() {
			return shown;
		}

		@Override
		boolean acceptChild(ShownCheckpoint checkpoint) {
			if (checkpoint.getLevel() > level) {
				return true;
			} else {
				return false;
			}
		}
	}


	private static final class Session {
		private Deque<Ancestor> currentBranch = new ArrayDeque<>();
		ShownReport result;

		Session(ShownReport report) {
			result = report;
			currentBranch.addLast(new ReportAncestor(report));
		}

		void handleCheckpoint(ShownCheckpoint checkpoint, boolean shown) {
			updateCurrentBranch(checkpoint);
			System.out.println(String.format("Checkpoint [%s] has parent [%s]", checkpoint.getName(), currentBranch.getLast().contents.getName()));
			if (shown) {
				showChild(checkpoint);
			}
			currentBranch.addLast(new CheckpointAncestor(checkpoint, shown));
		}

		void updateCurrentBranch(ShownCheckpoint checkpoint) {
			Ancestor parent = currentBranch.getLast();
			while (!parent.acceptChild(checkpoint)) {
				currentBranch.removeLast();
				parent = currentBranch.getLast();
			}
		}

		void showChild(ShownCheckpoint checkpoint) {
			// Head of currentBranch is parent of checkpoint.
			Iterator<Ancestor> it = currentBranch.descendingIterator();
			while (it.hasNext()) {
				Ancestor shownParent = it.next();
				if (shownParent.isShown()) {
					shownParent.contents.addChild(checkpoint);
					return;
				}
			}
		}
	}

	public ShownReport transform(Report report, View view) {
		ShownReport shownReport = new ShownReport();
		copyReport(report, shownReport);
		Session session = new Session(shownReport);
		for (Checkpoint checkpoint: report.getCheckpoints()) {
			boolean shown = view.showCheckpoint(report, checkpoint);
			ShownCheckpoint shownCheckpoint = new ShownCheckpoint();
			copyCheckpoint(checkpoint, shownCheckpoint);
			session.handleCheckpoint(shownCheckpoint, shown);
		}
		return session.result;
	}

	private void copyReport(Report source, ShownReport dest) {
		dest.setName(source.getName());
		dest.setDescription(source.getDescription());
		dest.setPath(source.getPath());
		dest.setStubStrategy(source.getStubStrategy());
		dest.setLinkMethod(source.getLinkMethod());
		dest.setTransformation(source.getTransformation());
		dest.setStorageId(source.getStorageId());
		dest.setStorageName(source.getStorage().getName());
		dest.setEstimatedMemoryUsage(source.getEstimatedMemoryUsage());
		dest.setCorrelationId(source.getCorrelationId());
		validator.validateObject(dest);
	}

	private void copyCheckpoint(Checkpoint source, ShownCheckpoint dest) {
		dest.setName(source.getName());
		dest.setMessage(source.getMessage());
		dest.setEncoding(source.getEncoding());
		dest.setMessageContext(source.getMessageContext());
		dest.setType(source.getType());
		dest.setLevel(source.getLevel());
		dest.setStub(source.getStub());
		dest.setStubbed(source.isStubbed());
		dest.setStubNotFound(source.getStubNotFound());
		dest.setPreTruncatedMessageLength(source.getPreTruncatedMessageLength());
		dest.setTypeAsString(source.getTypeAsString());
		dest.setThreadName(source.getThreadName());
		dest.setSourceClassName(source.getSourceClassName());
		dest.setMessageClassName(source.getMessageClassName());
		dest.setUid(source.getUid());
		validator.validateObject(dest);
	}
}
