"use client";

import Autocomplete from "@mui/material/Autocomplete";
import Box from "@mui/material/Box";
import CircularProgress from "@mui/material/CircularProgress";
import InputAdornment from "@mui/material/InputAdornment";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { MapPin, Search } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { useEffect, useMemo, useState } from "react";

import {
  fetchPublicSearchSuggestions,
  type PublicSearchSuggestion,
  type PublicSearchSuggestionKind,
} from "./public-search-api";

const SUGGESTION_DEBOUNCE_MS = 160;

export interface PublicSearchAutocompleteProps {
  ariaLabel: string;
  defaultValue?: string;
  kind: PublicSearchSuggestionKind;
  label?: string;
  name: "location" | "q";
  placeholder: string;
}

/**
 * Campo libre con sugerencias remotas reutilizable por inicio, filtros desktop y modal móvil.
 * Cancela la petición anterior, no consulta con menos de dos caracteres y conserva el submit GET.
 */
export function PublicSearchAutocomplete({
  ariaLabel,
  defaultValue = "",
  kind,
  label,
  name,
  placeholder,
}: PublicSearchAutocompleteProps) {
  const locale = useLocale();
  const t = useTranslations("SearchSuggestions");
  const [hasUserEdited, setHasUserEdited] = useState(false);
  const [inputValue, setInputValue] = useState(defaultValue);
  const [loading, setLoading] = useState(false);
  const [options, setOptions] = useState<PublicSearchSuggestion[]>([]);
  const normalizedInput = useMemo(() => inputValue.trim(), [inputValue]);

  useEffect(() => setInputValue(defaultValue), [defaultValue]);

  useEffect(() => {
    if (!hasUserEdited || normalizedInput.length < 2) {
      setOptions([]);
      setLoading(false);
      return;
    }

    const controller = new AbortController();
    const timer = window.setTimeout(() => {
      setLoading(true);
      void fetchPublicSearchSuggestions(locale, kind, normalizedInput, controller.signal)
        .then(setOptions)
        .catch((error: unknown) => {
          if (!(error instanceof DOMException && error.name === "AbortError")) {
            setOptions([]);
          }
        })
        .finally(() => {
          if (!controller.signal.aborted) {
            setLoading(false);
          }
        });
    }, SUGGESTION_DEBOUNCE_MS);

    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [hasUserEdited, kind, locale, normalizedInput]);

  const Icon = kind === "location" ? MapPin : Search;

  return (
    <Autocomplete
      autoComplete
      autoHighlight
      clearOnBlur={false}
      filterOptions={(values) => values}
      freeSolo
      getOptionKey={(option) =>
        typeof option === "string" ? option : `${option.kind}:${option.value}`
      }
      getOptionLabel={(option) => (typeof option === "string" ? option : option.label)}
      includeInputInList
      inputValue={inputValue}
      loading={loading}
      loadingText={t("loading")}
      noOptionsText={normalizedInput.length < 2 ? t("minimumCharacters") : t("noOptions")}
      onChange={(_event, value) => {
        setInputValue(typeof value === "string" ? value : (value?.value ?? ""));
      }}
      onInputChange={(_event, value, reason) => {
        if (reason !== "reset") {
          setHasUserEdited(true);
          setInputValue(value);
        }
      }}
      options={options}
      renderInput={(params) => (
        <TextField
          {...params}
          fullWidth
          label={label}
          placeholder={placeholder}
          slotProps={{
            htmlInput: {
              ...params.slotProps.htmlInput,
              "aria-label": ariaLabel,
              maxLength: 80,
              name,
            },
            input: {
              ...params.slotProps.input,
              endAdornment: (
                <>
                  {loading ? <CircularProgress color="inherit" size={18} /> : null}
                  {params.slotProps.input.endAdornment}
                </>
              ),
              startAdornment: (
                <InputAdornment position="start">
                  <Icon aria-hidden="true" size={18} strokeWidth={1.9} />
                </InputAdornment>
              ),
            },
          }}
        />
      )}
      renderOption={(props, option) => (
        <Box component="li" {...props} key={`${option.kind}:${option.value}`}>
          <Box sx={{ minWidth: 0 }}>
            <Typography noWrap sx={{ fontWeight: 600 }}>
              {option.label}
            </Typography>
            {option.context ? (
              <Typography color="text.secondary" noWrap variant="body2">
                {option.context}
              </Typography>
            ) : null}
          </Box>
        </Box>
      )}
      selectOnFocus
    />
  );
}
