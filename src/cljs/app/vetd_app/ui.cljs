
(ns vetd-app.ui
  (:require cljsjs.semantic-ui-react
            goog.object
            cljsjs.toastr
            [re-frame.core :as rf]))

;; handle to top-level extern from Semantic UI React
(def semantic-ui js/semanticUIReact)

(defn component
  "Get a component from sematic-ui-react:
    (component \"Button\")
    (component \"Menu\" \"Item\")"
  [k & ks]
  (if (seq ks)
    (apply goog.object/getValueByKeys semantic-ui k ks)
    (goog.object/get semantic-ui k)))

(def Grid (component "Grid"))
(def GridColumn (component "Grid" "Column"))
(def GridRow (component "Grid" "Row"))

(def Menu (component "Menu"))
(def MenuItem (component "Menu" "Item"))
(def MenuMenu (component "Menu" "Menu"))

(def Container (component "Container"))
(def Segment (component "Segment"))
(def SegmentInline (component "Segment" "Inline"))
(def Divider (component "Divider"))
(def Header (component "Header"))

(def Form (component "Form"))
(def FormField (component "Form" "Field"))

(def Button (component "Button"))
(def ButtonGroup (component "Button" "Group"))
(def ButtonOr (component "Button" "Or"))
(def Input (component "Input"))
(def TextArea (component "TextArea"))
(def Checkbox (component "Checkbox"))
(def Select (component "Select"))

(def Icon (component "Icon"))

(def Label (component "Label"))
(def LabelDetail (component "Label" "Detail"))
(def Message (component "Message"))

(def Image (component "Image"))

(def Loader (component "Loader"))

(def Item (component "Item"))
(def ItemGroup (component "ItemGroup"))
(def ItemImage (component "ItemImage"))
(def ItemContent (component "ItemContent"))
(def ItemHeader (component "ItemHeader"))
(def ItemMeta (component "ItemMeta"))
(def ItemDescription (component "ItemDescription"))
(def ItemExtra (component "ItemExtra"))


;; todo: add to init event? maybe not.
(aset js/toastr "options"
      (clj->js {:closeButton false,
                :debug false,
                :newestOnTop false,
                :positionClass "toast-top-center",
                :preventDuplicates false,
                :showDuration "300",
                :hideDuration "1000",
                :timeOut "5000",
                :extendedTimeOut "1000",
                :showEasing "swing",
                :hideEasing "linear",
                :showMethod "fadeIn",
                :hideMethod "fadeOut"}))


(rf/reg-fx
 :toast      ; todo: assumes "success"
 (fn [{:keys [type title message]}]
   (js/toastr.success message title)))
