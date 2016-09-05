package club.zhcs.thunder.module.qa;

import org.nutz.http.Http;
import org.nutz.http.Response;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.json.Json;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;
import org.nutz.mvc.ViewModel;
import org.nutz.mvc.annotation.At;
import org.nutz.mvc.annotation.Attr;
import org.nutz.mvc.annotation.Filters;
import org.nutz.mvc.annotation.GET;
import org.nutz.mvc.annotation.Ok;
import org.nutz.mvc.annotation.POST;
import org.nutz.mvc.annotation.Param;

import club.zhcs.thunder.Application;
import club.zhcs.thunder.Application.SessionKeys;
import club.zhcs.thunder.bean.qa.Nutzer;
import club.zhcs.thunder.biz.qa.NutzerService;
import club.zhcs.titans.nutz.module.base.AbstractBaseModule;
import club.zhcs.titans.utils.db.Result;

/**
 * @author Kerbores(kerbores@gmail.com)
 *
 * @project thunder-web
 *
 * @file WechatQAModule.java
 *
 * @description WechatQAModule.java
 *
 * @time 2016年9月5日 上午9:45:45
 *
 */
@At("qa")
// @Filters({ @By(type = CheckSession.class, args = {
// SessionKeys.WECHAT_USER_KEY, "/qa/bind" }) })
@Filters
public class WechatQAModule extends AbstractBaseModule {

	@Inject
	NutzerService nutzerService;

	@At
	@GET
	@Ok("beetl:pages/qa/topic.html")
	public Result topic(@Param(value = "page", df = "1") int page, @Param("tab") String tab, @Param("tag") String tag, @Param("search") String search,
			@Param(value = "limit", df = "15") int limit) {
		String topicApi = "https://nutz.cn/yvr/api/v1/topics?page=" + page + "&limit=" + limit;
		if (Strings.isNotBlank(tab)) {
			topicApi += "&tab=" + tab;
		}
		if (Strings.isNotBlank(tag)) {
			topicApi += "&tab=" + tag;
		}
		if (Strings.isNotBlank(search)) {
			topicApi += "&tab=" + search;
		}
		return Result.success().addData("topics", Json.fromJson(Http.get(topicApi).getContent())).addData("page", page).addData("tab", tab).addData("tag", tag)
				.addData("search", search).addData("limit", limit);
	}

	@At("/topic/json")
	@POST
	public Result topicJson(@Param(value = "page", df = "1") int page, @Param("tab") String tab, @Param("tag") String tag, @Param("search") String search,
			@Param(value = "limit", df = "15") int limit) {
		String topicApi = "https://nutz.cn/yvr/api/v1/topics?page=" + page + "&limit=" + limit;
		if (Strings.isNotBlank(tab)) {
			topicApi += "&tab=" + tab;
		}
		if (Strings.isNotBlank(tag)) {
			topicApi += "&tab=" + tag;
		}
		if (Strings.isNotBlank(search)) {
			topicApi += "&tab=" + search;
		}
		return Result.success().addData("topics", Json.fromJson(Http.get(topicApi).getContent())).addData("page", page).addData("tab", tab).addData("tag", tag)
				.addData("search", search).addData("limit", limit);
	}

	@At("/topic/detail/*")
	@Ok("beetl:pages/qa/topic_detail.html")
	public Result detail(String id, @Attr(Application.SessionKeys.WECHAT_USER_KEY) Nutzer nutzer) {
		return Result.success().addData("topic", Json.fromJson(Http.get("https://nutz.cn/yvr/api/v1/topic/" + id).getContent())).addData("reply",
				nutzer != null && Strings.isNotBlank(nutzer.getAccessToken()));
	}

	@At
	@Ok("re:beetl:pages/qa/bind.html")
	public String me(@Attr(SessionKeys.WECHAT_USER_KEY) Nutzer nutzer, ViewModel model) {
		if (nutzer == null || Strings.isBlank(nutzer.getAccessToken())) {
			return null;
		}
		Response r1 = Http.get("https://nutz.cn/yvr/api/v1/user/" + nutzer.getLoginName());
		NutMap userInfo = Lang.map(r1.getContent());
		model.putAll(userInfo);
		return "beetl:pages/qa/me.html";
	}
	
	@At("/reply/*")
	@GET
	@Ok("beetl:pages/qa/reply.html")
	public Result reply(String id) {
		return Result.success().addData("id", id);
	}

	@At
	public Result bind(@Param("token") String token, @Attr(Application.SessionKeys.WECHAT_USER_KEY) Nutzer nutzer) {
		Response response = Http.post2("https://nutz.cn/yvr/api/v1/accesstoken", NutMap.NEW().addv("accesstoken", token), 5000);
		if (response.isOK()) {
			NutMap data = Lang.map(response.getContent());
			if (data.getBoolean("success")) {
				// 更新信息到NUTZER
				Response r1 = Http.get("https://nutz.cn/yvr/api/v1/user/" + data.getString("loginname"));
				NutMap userInfo = Lang.map(r1.getContent());
				nutzer.setAccessToken(token);
				nutzer.setLoginName(userInfo.getAs("data", NutMap.class).getString("loginname"));
				nutzer.setAvatarUrl(userInfo.getAs("data", NutMap.class).getString("avatar_url"));
				// TODO 其他信息
				nutzerService.update(nutzer);
			}
			return Result.success().addData(data);
		}
		return Result.fail("token不正确!");
	}

}
