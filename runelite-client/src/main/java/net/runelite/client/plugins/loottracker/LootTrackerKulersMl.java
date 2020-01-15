package net.runelite.client.plugins.loottracker;

import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import org.json.JSONObject;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LootTrackerKulersMl {

	@Inject
	private LootTrackerConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	private HttpURLConnection con;

	String lootBuilder(String killerName, String npcName, String npcLvl, LootTrackerItem[] items) {
		if (!config.getApiEnabled()) return "";

		JSONObject loot = new JSONObject();
		loot.put("api_token", config.getApiKey());
		loot.put("npc", new JSONObject().put("killer", killerName).put("npc_name", npcName).put("npc_level", npcLvl));

		List<JSONObject> drops = new ArrayList<JSONObject>();
		for (LootTrackerItem i : items) {

			JSONObject s = new JSONObject();
			s.put("item_id", i.getId());
			s.put("item_qty", i.getQuantity());
			s.put("item_name", i.getName());

			drops.add(s);
		}
		loot.put("drops", drops);

		return loot.toString();
	}

	void postLoot(String jsonString) {
		String url = "http://kulers.ml/api/loot/post";
		System.out.println(jsonString);
		String urlParameters = "q=" + jsonString;
		byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

		try {

			URL myurl = new URL(url);
			con = (HttpURLConnection) myurl.openConnection();

			con.setDoOutput(true);
			con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent", "Java client");
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			try (DataOutputStream wr =
						 new DataOutputStream(con.getOutputStream())) {
				wr.write(postData);
			}

			StringBuilder content;

			try (BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()))) {

				String line;
				content = new StringBuilder();

				while ((line = in.readLine()) != null) {
					content.append(line);
					content.append(System.lineSeparator());
				}
			}

		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			con.disconnect();
		}
	}

	public void sendChatMessage(String chatMessage) {
		final String message = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT)
			.append(chatMessage)
			.build();

		chatMessageManager.queue(
			QueuedMessage.builder()
				.type(ChatMessageType.GAMEMESSAGE)
				.runeLiteFormattedMessage(message)
				.build());
	}
}
