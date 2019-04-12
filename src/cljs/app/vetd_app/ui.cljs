(ns vetd-app.ui
  (:require cljsjs.semantic-ui-react
            goog.object
            cljsjs.toastr
            [reagent.core :as r]
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

;; Layout
(def Grid (component "Grid"))
(def GridColumn (component "Grid" "Column"))
(def GridRow (component "Grid" "Row"))
(def Container (component "Container"))
(def Segment (component "Segment"))
(def SegmentInline (component "Segment" "Inline"))
(def Divider (component "Divider"))
(def Header (component "Header"))
(def Item (component "Item"))
(def ItemGroup (component "ItemGroup"))
(def ItemImage (component "ItemImage"))
(def ItemContent (component "ItemContent"))
(def ItemHeader (component "ItemHeader"))
(def ItemMeta (component "ItemMeta"))
(def ItemDescription (component "ItemDescription"))
(def ItemExtra (component "ItemExtra"))

;; Label
(def Label (component "Label"))
(def LabelDetail (component "Label" "Detail"))
(def Popup (component "Popup"))
(def Icon (component "Icon"))
(def Message (component "Message"))

;; Menu
(def Menu (component "Menu"))
(def MenuItem (component "Menu" "Item"))
(def MenuMenu (component "Menu" "Menu"))

;; Form
(def Form (component "Form"))
(def FormField (component "Form" "Field"))
(def FormGroup (component "Form" "Group"))
(def FormInput (component "Form" "Input"))
(def FormTextArea (component "Form" "TextArea"))
(def FormSelect (component "Form" "Select"))
(def FormButton (component "Form" "Button"))
(def Button (component "Button"))
(def ButtonGroup (component "Button" "Group"))
(def ButtonOr (component "Button" "Or"))

;; WARNING: use "input" (lowercase) if you need to handle value updates from an atom/sub (controlled elements). This "Input" (capitalized) is safe for uncontrolled elements.
(def Input (component "Input"))

(defn input
  "Creates a controlled text input with limited support for Semantic UI features.
  Fixes the bug where value updates cause cursor position to move to end of input."
  [{:keys [value on-change class size icon autoFocus spellCheck placeholder]}]
  [:div.ui.input
   {:class (str class " "
                size " "
                (when icon "icon"))}
   [:input {:type "text"
            :value value
            :autoFocus autoFocus
            :spellCheck spellCheck
            :on-change on-change
            :placeholder placeholder}]
   (when icon [:> Icon {:name icon}])])

(def TextArea (component "TextArea"))
(def Checkbox (component "Checkbox"))
(def Select (component "Select"))
(def Dropdown (component "Dropdown"))

;; Misc
(def Image (component "Image"))
(def Step (component "Step"))
(def StepGroup (component "Step" "Group"))
(def StepContent (component "Step" "Content"))
(def StepTitle (component "Step" "Title"))
(def StepDescription (component "Step" "Description"))

;; Accordion
(def Accordion (component "Accordion"))
(def AccordionAccordion (component "Accordion" "Accordion"))
(def AccordionPanel (component "Accordion" "Panel"))
(def AccordionContent (component "Accordion" "Content"))
(def AccordionTitle (component "Accordion" "Title"))

(defn nx-accordion-item
  "Define an item within a non-exclusive accordion."
  [title & body]
  (let [active& (r/atom false)]
    (fn [title & body]
      [:div
       [:> AccordionTitle {:active @active&
                           :onClick (fn [_ this] (swap! active& not))}
        title]
       [:> AccordionContent {:active @active&}
        (for [b body]
          b)]])))

(defn ^:private nx-any-accordion
  "Build any type of non-exclusive accordion."
  [component & accordion-items]
  (fn [& accordion-items]
    [:> component
     (for [ai accordion-items]
       ai)]))

;; Non-Exclusive Accordion (you can have multiple items remain open concurrently)
(def nx-accordion (partial nx-any-accordion Accordion))
(def nx-sub-accordion (partial nx-any-accordion AccordionAccordion))

;; Modal
(def Modal (component "Modal"))
(def ModalHeader (component "Modal" "Header"))
(def ModalContent (component "Modal" "Content"))
(def ModalActions (component "Modal" "Actions"))
(def Confirm (component "Confirm"))

;; Toastr
;; setup Toastr config:
(aset js/toastr "options"         ; TODO add to init event? maybe not.
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
