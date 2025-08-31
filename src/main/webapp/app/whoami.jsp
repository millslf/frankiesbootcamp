<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%
  response.setHeader("Cache-Control","no-store, no-cache, must-revalidate");
  response.setHeader("Pragma","no-cache");
%>
<%!
  // Minimal JSON string escaper
  private static String jsonEscape(String s) {
    if (s == null) return "";
    StringBuilder sb = new StringBuilder(s.length() + 16);
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"':  sb.append("\\\""); break;
        case '\\': sb.append("\\\\"); break;
        case '\b': sb.append("\\b");  break;
        case '\f': sb.append("\\f");  break;
        case '\n': sb.append("\\n");  break;
        case '\r': sb.append("\\r");  break;
        case '\t': sb.append("\\t");  break;
        default:
          if (c < 0x20) {
            String hex = Integer.toHexString(c);
            sb.append("\\u");
            for (int k = hex.length(); k < 4; k++) sb.append('0');
            sb.append(hex);
          } else {
            sb.append(c);
          }
      }
    }
    return sb.toString();
  }
%>
<%
  String name  = (String) session.getAttribute("athleteName");
  String email = (String) session.getAttribute("athleteEmail");
  boolean authed = (email != null);

  StringBuilder json = new StringBuilder(96);
  json.append("{\"auth\":").append(authed ? "true" : "false");
  if (authed) {
    if (name != null)  json.append(",\"name\":\"").append(jsonEscape(name)).append("\"");
    json.append(",\"email\":\"").append(jsonEscape(email)).append("\"");
  }
  json.append('}');

  out.write(json.toString());
%>