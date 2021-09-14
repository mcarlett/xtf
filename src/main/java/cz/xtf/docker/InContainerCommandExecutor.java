package cz.xtf.docker;

import cz.xtf.XTFConfiguration;
import cz.xtf.openshift.OpenShiftBinaryClient;
import io.fabric8.kubernetes.api.model.Pod;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class InContainerCommandExecutor {
	private DockerContainer dockerContainer;
	private Pod pod;
	private OpenShiftBinaryClient client;

	/**
	 * Creates instance connected to specified pod.
	 *
	 * @param pod in which commands should be executed
	 * @return new class instance
	 */
	public static InContainerCommandExecutor inPod(Pod pod) {
		return new InContainerCommandExecutor(pod);
	}

	private InContainerCommandExecutor(Pod pod) {
		dockerContainer = DockerContainer.createForPod(pod);
		this.pod = pod;
		this.client = OpenShiftBinaryClient.getInstance();
		this.client.project(XTFConfiguration.buildNamespace());
	}

	/**
	 * Returns content of directory specified by path without hidden dirs. Runs "ls -1 path" command in container
	 * and splits result by new line.
	 *
	 * @param path which will be queried
	 * @return directory content
	 */
	public List<String> listDirContent(String path) {
		String cmd = String.format("ls -1 %s", path);
		return executeBashCommandAndExpectList(cmd);
	}

	/**
	 * Returns content of directory specified by path with hidden dirs. Runs "ls -a1 path" command in container
	 * and splits result by new line.
	 *
	 * @param path which will be queried
	 * @return directory content
	 */
	public List<String> listFullDirContent(String path) {
		String cmd = String.format("ls -a1 %s", path);
		return executeBashCommandAndExpectList(cmd);
	}

	/**
	 * Executes env command in container and parses result.
	 *
	 * @return map of environments in container
	 */
	public Map<String, String> listEnvs() {
		Map<String, String> containerEnv = new HashMap<>();

		Stream.of(executeCommand("env").split("\n")).forEach(env -> {
			String[] parsedEnv = env.split("=", 2);
			containerEnv.put(parsedEnv[0], parsedEnv[1]);
		});

		return containerEnv;
	}

	/**
	 * Executes given bash command. (Wraps given command with "bash -c "\command"\").
	 *
	 * @param command that will be executed as bash command
	 * @return whatever the result is
	 */
	public String executeBashCommand(String command) {
		//String cmd = String.format("bash -c \"%s\"", command);
		//return executeCommand(cmd);
		return executeCommand(command, false);
	}

	public String executeCommand(String command) {
		return this.executeCommand(command, false);
	}

	/**
	 * Executes given command as "docker exec containerID command".
	 *
	 * @param command to be executed
	 * @return whatever is the result
	 */
	public String executeCommand(String command, final boolean readError) {
		final String errorMsg = "error on running command on pod " + pod.getMetadata().getName();
		final List<String> args = new ArrayList<>();
		args.addAll(Arrays.asList("exec", pod.getMetadata().getName(), "--"));
		if(command.contains("|")) {
			args.add(command);
		} else {
			args.addAll(Arrays.asList(command.split(" ")));
		}

		return this.client.executeCommandWithReturn(readError, errorMsg, args.toArray(new String[]{}));
	}

	/**
	 * Executes bash command in container and parses the result by new lines.
	 * (Wraps given command with "bash -c "\command"\").
	 *
	 * @param command to be executed
	 * @return output parsed by new lines
	 */
	public List<String> executeBashCommandAndExpectList(String command) {
		//String cmd = String.format("bash -c \"%s\"", command);
		//return executeCommandAndExpectList(cmd);
		return executeCommandAndExpectList(false, command);
	}

	public List<String> executeCommandAndExpectList(String command) {
		return executeCommandAndExpectList(false, command);
	}

	/**
	 * Executes given command as "docker exec containerID command" and parses the result by new line.
	 *
	 * @param command that will be executed
	 * @return output parsed by new lines
	 */
	public List<String> executeCommandAndExpectList(final boolean readError, String command) {
		String[] result = StringUtils.split(executeCommand(command, readError), '\n');
		return Arrays.asList(result);
	}
}
