package com.zyao89.view.zweb.javascript;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import com.zyao89.view.zweb.annotations.ZCmd;
import com.zyao89.view.zweb.annotations.ZFunction;
import com.zyao89.view.zweb.annotations.ZMethod;
import com.zyao89.view.zweb.constants.InternalConstantName;
import com.zyao89.view.zweb.constants.InternalFunctionName;
import com.zyao89.view.zweb.exceptions.ZWebException;
import com.zyao89.view.zweb.inter.IZWebHandler;
import com.zyao89.view.zweb.inter.IZWebMessageController;
import com.zyao89.view.zweb.inter.IZWebMethodInterface;
import com.zyao89.view.zweb.inter.IZWebOnStateListener;
import com.zyao89.view.zweb.utils.JsUtils;
import com.zyao89.view.zweb.utils.ZLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 基本暴露接口
 *
 * @author Zyao89
 * 2017/11/6.
 */
/* package */ class ZJavaScript implements IZRenderListener
{
    @NonNull
    private final IZWebHandler         mZWebHandler;
    private       IZWebMethodInterface mZWebMethodInterface;
    private       IZWebOnStateListener mZWebStateListener;

    /* package */ ZJavaScript(@NonNull IZWebHandler zWeb)
    {
        mZWebHandler = zWeb;
    }

    public void setNativeMethodImplement(@NonNull IZWebMethodInterface interfaceObj)
    {
        mZWebMethodInterface = interfaceObj;
    }

    public void setOnStateListener(@NonNull IZWebOnStateListener interfaceObj)
    {
        mZWebStateListener = interfaceObj;
    }

    @NonNull
    protected final IZWebMethodInterface getZWebMethodInterface()
    {
        if (mZWebMethodInterface == null)
        {
            ZLog.with(this).w("IZWebMethodInterface 未实现，现调用 setNativeMethodImplement()...");
        }
        return mZWebMethodInterface;
    }

    @NonNull
    protected final IZWebOnStateListener getZWebStateListener()
    {
        if (mZWebStateListener == null)
        {
            ZLog.with(this).w("IZWebOnStateListener 未实现，现调用 setOnStateListener()...");
        }
        return mZWebStateListener;
    }

    @NonNull
    protected final IZWebHandler getZWebHandler()
    {
        return mZWebHandler;
    }

    protected final String getFrameworkID()
    {
        return getZWebHandler().getFrameworkUUID();
    }

    protected final boolean execJS(String function, JSONObject json)
    {
        return mZWebHandler.execJS(function, json);
    }

    @Override
    @JavascriptInterface
    public void onCreated (String frameworkID, String oSize)
    {
        JSONObject jsonObject = JsUtils.json2Obj(oSize);
        String width = jsonObject.optString(InternalConstantName.PARAM_WIDTH);
        String height = jsonObject.optString(InternalConstantName.PARAM_HEIGHT);

        getZWebStateListener().onZWebCreated(getZWebHandler(), Integer.parseInt(width, 10), Integer.parseInt(height, 10));
    }

    @Override
    @JavascriptInterface
    public void onException (String frameworkID, long errCode, String oMsg)
    {
        getZWebStateListener().onZWebException(getZWebHandler(), errCode, oMsg);
    }

    @Override
    @JavascriptInterface
    public void onRequire (String frameworkID, String oJson)
    {
        JSONObject jsonObject = JsUtils.json2Obj(oJson);
        final String sequence = jsonObject.optString(InternalConstantName.SEQUENCE);
        final String url = jsonObject.optString(InternalConstantName.URL);
        final String method = jsonObject.optString(InternalConstantName.METHOD);
        final String data = jsonObject.optString(InternalConstantName.DATA);
        final String type = jsonObject.optString(InternalConstantName.TYPE);

        final IZWebOnStateListener.IZRequireController zController = new ZRequireController(InternalFunctionName.REQUIRE_CALLBACK, sequence);
        getZWebStateListener().onZWebRequire(getZWebHandler(), url, method, data, type, zController);
    }

    @Override
    @JavascriptInterface
    public void onMessage (String frameworkID, String oJson)
    {
        JSONObject jsonObject = JsUtils.json2Obj(oJson);
        final String sequence = jsonObject.optString(InternalConstantName.SEQUENCE);
        final String cmd = jsonObject.optString(InternalConstantName.CMD);
        final String data = jsonObject.optString(InternalConstantName.DATA);

        final IZWebOnStateListener.IZMessageController zController = new ZMessageController(InternalFunctionName.MESSAGE_CALLBACK, sequence, cmd, data);
        getZWebStateListener().onZWebMessage(getZWebHandler(), cmd, data, zController);
    }

    @Override
    @JavascriptInterface
    public void onDestroy (String frameworkID)
    {
        getZWebStateListener().onZWebDestroy(getZWebHandler());
    }

    @Override
    @JavascriptInterface
    public void onLog (String frameworkID, String oData)
    {
        JSONObject jsonObject = JsUtils.json2Obj(oData);
        String type = jsonObject.optString(InternalConstantName.PARAM_TYPE);
        String msg = jsonObject.optString(InternalConstantName.PARAM_MSG);

        getZWebStateListener().onZWebLog(getZWebHandler(), type, msg);
    }

    class ZRequireController implements IZWebOnStateListener.IZRequireController
    {
        final String mFunctionName;
        final String mSequence;

        ZRequireController(String function, String sequence)
        {
            this.mFunctionName = function;
            this.mSequence = sequence;
        }

        @Override
        public void result(boolean isSuccess)
        {
            JSONObject json = convert(this.mSequence, isSuccess, null);
            ZJavaScript.this.execJS(this.mFunctionName, json);
        }

        @Override
        public void result(boolean isSuccess, String data)
        {
            JSONObject json = convert(this.mSequence, isSuccess, data);
            ZJavaScript.this.execJS(this.mFunctionName, json);
        }

        @Override
        public void result(boolean isSuccess, @NonNull JSONObject data)
        {
            JSONObject json = convert(this.mSequence, isSuccess, data.toString());
            ZJavaScript.this.execJS(this.mFunctionName, json);
        }

        private JSONObject convert(@NonNull String sequence, boolean isSuccess, @Nullable String data)
        {
            try
            {
                JSONObject json = new JSONObject();
                json.put(InternalConstantName.SEQUENCE, sequence);
                json.put(InternalConstantName.RESULT, isSuccess ? InternalConstantName.SUCCESS : InternalConstantName.ERROR);
                if (data != null)
                {
                    json.put(InternalConstantName.DATA, data);
                }
                return json;
            }
            catch (JSONException e)
            {
                throw new ZWebException(this.mFunctionName + " --> ZRequireController convert is Failed... ", e);
            }
        }
    }

    class ZMessageController extends ZRequireController implements IZWebOnStateListener.IZMessageController, IZWebMessageController
    {
        private final String mCmd;
        private final String mData;

        ZMessageController(String function, String sequence, String cmd, String data)
        {
            super(function, sequence);
            this.mCmd = cmd;
            this.mData = data;
        }

        @Override
        public <T> void parseMessage(@NonNull T object)
        {
            Method finalM = null;
            Method[] methods = object.getClass().getDeclaredMethods();
            for (Method method : methods)
            { // 以注解为主
                if (method.getDeclaringClass() == Object.class)
                {
                    continue;
                }
                Annotation[] methodAnnotations = method.getAnnotations();
                if (methodAnnotations.length > 0)
                { // 以注解为主
                    for (Annotation annotation : methodAnnotations)
                    {
                        if (parseCmdAnnotation(annotation))
                        {
                            finalM = method;
                            break;
                        }
                    }
                }
                if (finalM != null)
                {
                    break;
                }
            }
            if (finalM == null)
            { // 以方法名
                for (Method method : methods)
                {
                    if (method.getDeclaringClass() == Object.class)
                    {
                        continue;
                    }
                    if (method.getName().equals(this.mCmd))
                    {
                        finalM = method;
                    }
                    if (finalM != null)
                    {
                        break;
                    }
                }
            }
            if (finalM != null)
            {
                finalM.setAccessible(true);
                try
                {//调用方法
                    Class<?>[] parameterTypes = finalM.getParameterTypes();
                    if (parameterTypes.length == 1)
                    {
                        //调用方法
                        Object result;
                        if (JSONObject.class.equals(parameterTypes[0]))
                        {
                            JSONObject jsonObject = new JSONObject(this.mData);
                            result = finalM.invoke(object, jsonObject);
                        }
                        else
                        {
                            result = finalM.invoke(object, this.mData);
                        }
                        if (result == null || result instanceof Void)
                        {
                            this.result(true);
                        }
                        else if (result instanceof String)
                        {
                            this.result(true, (String) result);
                        }
                        else if (result instanceof JSONObject)
                        {
                            this.result(true, (JSONObject) result);
                        }
                        else
                        {
                            throw new ZWebException("Not support return type...");
                        }
                    }
                    else if (parameterTypes.length == 2)
                    {
                        if (IZWebMessageController.class.equals(parameterTypes[1]))
                        {
                            if (JSONObject.class.equals(parameterTypes[0]))
                            {
                                JSONObject jsonObject = new JSONObject(this.mData);
                                finalM.invoke(object, jsonObject, this);
                            }
                            else
                            {
                                finalM.invoke(object, this.mData, this);
                            }
                        }
                        else
                        {
                            throw new ZWebException("The second parameter type is incorrect... Must be：" + IZWebMessageController.class);
                        }
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    this.result(false, e.getMessage());
                }
            }
            else
            {
                this.result(false, "Not Found CMD...");
            }
        }

        private boolean parseCmdAnnotation(Annotation annotation)
        {
            if (annotation instanceof ZFunction)
            {
                ZLog.with(this).w("Please use ZCmd...");
                return parseCmdName(((ZFunction) annotation).value());
            }
            else if (annotation instanceof ZMethod)
            {
                ZLog.with(this).w("Please use ZCmd...");
                return parseCmdName(((ZMethod) annotation).value());
            }
            else if (annotation instanceof ZCmd)
            {
                return parseCmdName(((ZCmd) annotation).value());
            }
            else
            {
                throw new ZWebException("Not Found annotation...");
            }
        }

        private boolean parseCmdName(String cmdName)
        {
            if (TextUtils.isEmpty(cmdName))
            {
                throw new ZWebException("ZMethod annotation Value is null");
            }
            return cmdName.equals(this.mCmd);
        }
    }
}
