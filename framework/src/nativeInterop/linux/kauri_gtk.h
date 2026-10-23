#ifndef KAURI_GTK_H
#define KAURI_GTK_H

#ifdef __cplusplus
extern "C" {
#endif

typedef int gboolean;
typedef unsigned int guint;
typedef unsigned long gulong;
typedef void *gpointer;
typedef struct _GtkWidget GtkWidget;
typedef struct _GtkWindow GtkWindow;
typedef struct _GtkContainer GtkContainer;
typedef struct _WebKitWebContext WebKitWebContext;
typedef struct _WebKitWebView WebKitWebView;
typedef struct _WebKitPolicyDecision WebKitPolicyDecision;
typedef struct _WebKitNavigationPolicyDecision WebKitNavigationPolicyDecision;
typedef struct _WebKitNavigationAction WebKitNavigationAction;
typedef struct _WebKitURIRequest WebKitURIRequest;
typedef struct _WebKitSettings WebKitSettings;
typedef struct { guint domain; int code; char *message; } GError;

int gtk_init_check(int *, char ***);
GtkWidget *gtk_window_new(int);
void gtk_window_set_title(GtkWindow *, const char *);
void gtk_window_set_default_size(GtkWindow *, int, int);
void gtk_container_add(GtkContainer *, GtkWidget *);
void gtk_widget_show_all(GtkWidget *);
void gtk_widget_destroy(GtkWidget *);
void gtk_main(void);
void gtk_main_quit(void);
guint gtk_main_level(void);
guint g_timeout_add_full(int, guint, gboolean (*)(gpointer), gpointer, void (*)(gpointer));
gboolean g_source_remove(guint);

WebKitWebContext *webkit_web_context_new_ephemeral(void);
guint webkit_network_error_quark(void);
guint webkit_policy_error_quark(void);
GtkWidget *webkit_web_view_new_with_context(WebKitWebContext *);
void webkit_web_view_load_uri(WebKitWebView *, const char *);
const char *webkit_web_view_get_uri(WebKitWebView *);
void webkit_policy_decision_ignore(WebKitPolicyDecision *);
WebKitURIRequest *webkit_navigation_policy_decision_get_request(WebKitNavigationPolicyDecision *);
const char *webkit_uri_request_get_uri(WebKitURIRequest *);
WebKitSettings *webkit_web_view_get_settings(WebKitWebView *);
void webkit_settings_set_javascript_can_open_windows_automatically(WebKitSettings *, gboolean);
gpointer g_object_ref_sink(gpointer);
void g_object_unref(gpointer);
void g_signal_handler_disconnect(gpointer, gulong);

gulong g_signal_connect_data(
    gpointer, const char *, void (*)(void), gpointer, void (*)(gpointer, gpointer), int);

#ifdef __cplusplus
}
#endif

#endif
